package nl.futureedge.jta4spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;


/**
 * A {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the jta4spring namespace.
 */
public class Jta4SpringNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("transaction-manager", new TransactionManagerParser());
		registerBeanDefinitionParser("data-source", new DataSourceParser());
		registerBeanDefinitionParser("connection-factory", new ConnectionFactoryParser());
	}

}
