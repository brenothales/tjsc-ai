package br.jus.tjsc.ai.agent.minuta.infrastructure.ai;

import br.jus.tjsc.ai.agent.minuta.domain.VersaoTipo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class RedacaoService {

    private final ChatClient chatClient;
    private final Resource   systemTpl;
    private final Resource   relatorioTpl;
    private final Resource   fundamentacaoTpl;
    private final Resource   dispositivoTpl;

    RedacaoService(@Qualifier("minutaChatClient") ChatClient chatClient,
                   @Value("classpath:prompts/redacao-system.st")    Resource systemTpl,
                   @Value("classpath:prompts/relatorio-user.st")     Resource relatorioTpl,
                   @Value("classpath:prompts/fundamentacao-user.st") Resource fundamentacaoTpl,
                   @Value("classpath:prompts/dispositivo-user.st")   Resource dispositivoTpl) {
        this.chatClient       = chatClient;
        this.systemTpl        = systemTpl;
        this.relatorioTpl     = relatorioTpl;
        this.fundamentacaoTpl = fundamentacaoTpl;
        this.dispositivoTpl   = dispositivoTpl;
    }

    private String system() {
        return new PromptTemplate(systemTpl).render();
    }

    String relatorio(String numero, String dados) {
        String user = new PromptTemplate(relatorioTpl)
                .render(Map.of("numero", numero, "dados", dados));
        return chatClient.prompt().system(system()).user(user).call().content();
    }

    String fundamentacao(String numero, VersaoTipo tipo, String dados, String relatorio) {
        String user = new PromptTemplate(fundamentacaoTpl)
                .render(Map.of(
                        "numero",        numero,
                        "tipoLabel",     tipo.label(),
                        "tipoInstrucao", tipo.instrucaoFundamentacao(),
                        "dados",         dados,
                        "relatorio",     relatorio));
        return chatClient.prompt().system(system()).user(user).call().content();
    }

    String dispositivo(String numero, VersaoTipo tipo, String dados,
                       String relatorio, String fundamentacao) {
        String user = new PromptTemplate(dispositivoTpl)
                .render(Map.of(
                        "numero",         numero,
                        "tipoLabel",      tipo.label(),
                        "tipoLabelUpper", tipo.label().toUpperCase(),
                        "dados",          dados,
                        "relatorio",      relatorio,
                        "fundamentacao",  fundamentacao));
        return chatClient.prompt().system(system()).user(user).call().content();
    }
}
