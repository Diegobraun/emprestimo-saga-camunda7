package br.com.diegobraun.emprestimo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.diegobraun.emprestimo.proposta.NovaProposta;
import br.com.diegobraun.emprestimo.proposta.PropostaAberta;
import br.com.diegobraun.emprestimo.proposta.PropostaService;
import br.com.diegobraun.emprestimo.proposta.Revisao;
import br.com.diegobraun.emprestimo.sistemas.Chaos;
import br.com.diegobraun.emprestimo.sistemas.ChaosService;
import br.com.diegobraun.emprestimo.sistemas.ContratoService;
import br.com.diegobraun.emprestimo.sistemas.CoreBancario;
import br.com.diegobraun.emprestimo.sistemas.LimiteService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.camunda.bpm.engine.test.assertions.bpmn.ProcessInstanceAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "camunda.bpm.job-execution.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:teste;DB_CLOSE_DELAY=-1"
})
class ContratacaoEmprestimoProcessTest {

    @Autowired
    ProcessEngine engine;

    @Autowired
    RuntimeService runtime;

    @Autowired
    ManagementService management;

    @Autowired
    PropostaService propostas;

    @Autowired
    LimiteService limites;

    @Autowired
    ContratoService contratos;

    @Autowired
    CoreBancario core;

    @Autowired
    ChaosService chaos;

    private final Map<String, ProcessInstance> instancias = new HashMap<>();

    @BeforeEach
    void preparar() {
        BpmnAwareTests.init(engine);
    }

    @AfterEach
    void limpar() {
        chaos.definir(Chaos.desligado());
        ClockUtil.reset();
    }

    @Test
    void aprovadaAutomaticamenteEAssinadaViraContrato() {
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-1");
        rodarJobs(p);
        bpmn(p).isWaitingAt("AguardarAssinatura");

        assertThat(propostas.assinar(p.propostaId())).isTrue();
        rodarJobs(p);

        bpmn(p).isEnded()
                .hasPassedInOrder("AnalisarCredito", "ReservarLimite", "GerarContrato", "AguardarAssinatura",
                        "Desembolsar", "AtivarContrato", "DesfechoContratado", "FimContratado");
        assertThat(contratos.contrato(p.propostaId())).map(ContratoService.Contrato::status).contains(ContratoService.Status.ATIVO);
        assertThat(core.lancamento(p.propostaId() + ":desembolso")).isPresent();
    }

    @Test
    void scoreBaixoRecusaSemTocarEmNada() {
        PropostaAberta p = abrir("cliente-ruim-" + UUID.randomUUID(), "10000", "conta-1");
        rodarJobs(p);

        bpmn(p).isEnded()
                .hasPassed("FimRecusadaCredito").hasNotPassed("ReservarLimite");
    }

    @Test
    void scoreMedioVaiParaAnalistaQueAprova() {
        PropostaAberta p = abrir("cliente-medio-" + UUID.randomUUID(), "10000", "conta-1");
        rodarJobs(p);
        bpmn(p).isWaitingAt("RevisarProposta");

        assertThat(propostas.revisar(p.propostaId(), new Revisao(true, "ok"))).isPresent();
        rodarJobs(p);

        bpmn(p).isWaitingAt("AguardarAssinatura");
    }

    @Test
    void assinaturaQueNaoChegaExpiraECompensa() {
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-1");
        rodarJobs(p);

        ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 3 * 60 * 60 * 1000L));
        rodarJobs(p);

        bpmn(p).isEnded()
                .hasPassed("LiberarLimite", "CancelarContrato", "FimExpirada").hasNotPassed("Desembolsar");
        assertThat(limites.reserva(p.propostaId())).map(LimiteService.Reserva::status).contains(LimiteService.Status.LIBERADA);
        assertThat(contratos.contrato(p.propostaId())).map(ContratoService.Contrato::status).contains(ContratoService.Status.CANCELADO);
    }

    @Test
    void desembolsoRecusadoCompensaReservaEContrato() {
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-que-nao-existe");
        rodarJobs(p);
        propostas.assinar(p.propostaId());
        rodarJobs(p);

        bpmn(p).isEnded()
                .hasPassed("LiberarLimite", "CancelarContrato", "FimCancelada");
    }

    @Test
    void contratoRecusadoDesfazSoAReserva() {
        PropostaAberta p = abrir("cliente-bom-restricao-" + UUID.randomUUID(), "10000", "conta-1");
        rodarJobs(p);

        bpmn(p).isEnded()
                .hasPassed("LiberarLimite", "FimContratoRecusado").hasNotPassed("CancelarContrato", "AguardarAssinatura");
        assertThat(limites.reserva(p.propostaId())).map(LimiteService.Reserva::status).contains(LimiteService.Status.LIBERADA);
    }

    @Test
    void assinaturaSemNinguemEsperandoEhDescartada() {
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-1");

        assertThat(propostas.assinar(p.propostaId())).isFalse();

        rodarJobs(p);
        assertThat(propostas.assinar(p.propostaId())).isTrue();
        assertThat(propostas.assinar(p.propostaId())).isFalse();
    }

    @Test
    void coreForaDoArViraIncidenteQueOperadorResolve() {
        chaos.definir(new Chaos(true, false));
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-2");
        rodarJobs(p);
        propostas.assinar(p.propostaId());

        for (int tentativa = 0; tentativa < 5 && incidentes(p).isEmpty(); tentativa++) {
            rodarJobs(p);
            ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 5_000));
        }
        assertThat(incidentes(p)).hasSize(1);
        assertThat(incidentes(p).getFirst().getActivityId()).isEqualTo("Desembolsar");

        chaos.definir(Chaos.desligado());
        Job job = management.createJobQuery().processInstanceId(p.processInstanceId()).singleResult();
        management.setJobRetries(job.getId(), 1);
        rodarJobs(p);

        bpmn(p).isEnded().hasPassed("FimContratado");
    }

    @Test
    void operadorCancelaPorVariavelERetryECompensa() {
        chaos.definir(new Chaos(true, false));
        PropostaAberta p = abrir("cliente-bom-" + UUID.randomUUID(), "10000", "conta-2");
        rodarJobs(p);
        propostas.assinar(p.propostaId());

        for (int tentativa = 0; tentativa < 5 && incidentes(p).isEmpty(); tentativa++) {
            rodarJobs(p);
            ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 5_000));
        }
        assertThat(incidentes(p)).hasSize(1);

        runtime.setVariable(p.processInstanceId(), "cancelarDesembolso", true);
        Job job = management.createJobQuery().processInstanceId(p.processInstanceId()).singleResult();
        management.setJobRetries(job.getId(), 1);
        rodarJobs(p);

        bpmn(p).isEnded().hasPassed("CancelarContrato", "LiberarLimite", "FimCancelada");
        assertThat(core.lancamento(p.propostaId() + ":desembolso")).isEmpty();
        assertThat(limites.reserva(p.propostaId())).map(LimiteService.Reserva::status).contains(LimiteService.Status.LIBERADA);
        assertThat(contratos.contrato(p.propostaId())).map(ContratoService.Contrato::status).contains(ContratoService.Status.CANCELADO);
    }

    private PropostaAberta abrir(String cliente, String valor, String conta) {
        PropostaAberta p = propostas.abrir(new NovaProposta(cliente, new BigDecimal(valor), 12, conta, "PT2H"));
        instancias.put(p.propostaId(), runtime.createProcessInstanceQuery().processInstanceId(p.processInstanceId()).singleResult());
        return p;
    }

    private ProcessInstanceAssert bpmn(PropostaAberta p) {
        return BpmnAwareTests.assertThat(instancias.get(p.propostaId()));
    }

    private List<Incident> incidentes(PropostaAberta p) {
        return runtime.createIncidentQuery().processInstanceId(p.processInstanceId()).list();
    }

    private void rodarJobs(PropostaAberta p) {
        for (int rodada = 0; rodada < 50; rodada++) {
            List<Job> prontos = management.createJobQuery().processInstanceId(p.processInstanceId())
                    .executable().list();
            if (prontos.isEmpty()) {
                return;
            }
            for (Job job : prontos) {
                try {
                    management.executeJob(job.getId());
                } catch (RuntimeException e) {
                    return;
                }
            }
        }
    }
}
