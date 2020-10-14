package dev.suvera.keycloak.scim2.storage.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * author: suvera
 * date: 10/14/2020 1:05 PM
 */
public class SkssApi implements Spi {

    public boolean isInternal() {
        return false;
    }

    public String getName() {
        return "skss";
    }

    public Class<? extends Provider> getProviderClass() {
        return SkssService.class;
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SkssServiceProviderFactory.class;
    }

}
