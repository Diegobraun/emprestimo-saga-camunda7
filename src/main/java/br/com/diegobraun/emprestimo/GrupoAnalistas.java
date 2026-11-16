package br.com.diegobraun.emprestimo;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GrupoAnalistas implements ApplicationRunner {

    public static final String GRUPO = "analistas";

    private final IdentityService identity;

    public GrupoAnalistas(IdentityService identity) {
        this.identity = identity;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (identity.createGroupQuery().groupId(GRUPO).count() == 0) {
            Group grupo = identity.newGroup(GRUPO);
            grupo.setName("Analistas de crédito");
            grupo.setType("WORKFLOW");
            identity.saveGroup(grupo);
        }
        if (identity.createUserQuery().userId("demo").memberOfGroup(GRUPO).count() == 0
                && identity.createUserQuery().userId("demo").count() == 1) {
            identity.createMembership("demo", GRUPO);
        }
    }
}
