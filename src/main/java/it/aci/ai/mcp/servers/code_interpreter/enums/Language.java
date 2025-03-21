package it.aci.ai.mcp.servers.code_interpreter.enums;

import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;
import it.aci.ai.mcp.servers.code_interpreter.services.providers.impl.JavaProvider;
import it.aci.ai.mcp.servers.code_interpreter.services.providers.impl.PythonProvider;
import it.aci.ai.mcp.servers.code_interpreter.services.providers.impl.TypescriptProvider;

public enum Language {

    PYTHON(PythonProvider.class), TYPESCRIPT(TypescriptProvider.class), JAVA(JavaProvider.class);

    private Language(Class<? extends LanguageProvider> provider) {
        this.provider = provider;
    }

    private Class<? extends LanguageProvider> provider;

    public Class<? extends LanguageProvider> getProvider() {
        return provider;
    }

}
