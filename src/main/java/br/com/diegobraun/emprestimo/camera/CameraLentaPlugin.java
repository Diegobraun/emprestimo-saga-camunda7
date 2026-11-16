package br.com.diegobraun.emprestimo.camera;

import java.util.ArrayList;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.springframework.stereotype.Component;

@Component
public class CameraLentaPlugin extends AbstractProcessEnginePlugin {

    private final CameraLenta camera;

    public CameraLentaPlugin(CameraLenta camera) {
        this.camera = camera;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        if (configuration.getCustomPreBPMNParseListeners() == null) {
            configuration.setCustomPreBPMNParseListeners(new ArrayList<>());
        }
        configuration.getCustomPreBPMNParseListeners().add(new AbstractBpmnParseListener() {
            @Override
            public void parseServiceTask(Element element, ScopeImpl scope, ActivityImpl activity) {
                activity.addListener(ExecutionListener.EVENTNAME_START, execution -> camera.pausar());
            }
        });
    }
}
