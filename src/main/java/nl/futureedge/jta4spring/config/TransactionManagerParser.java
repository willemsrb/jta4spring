package nl.futureedge.jta4spring.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import nl.futureedge.jta4spring.JtaMonitor;
import nl.futureedge.jta4spring.JtaTransactionManager;


/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that
 * parses an {@code transaction-manager} element and creates a {@link BeanDefinition}
 * for an {@link org.springframework.transaction.jta.JtaTransactionManager}.
 */
public class TransactionManagerParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
		// JTA-MONITOR
		final BeanDefinitionBuilder jtaMonitorBuilder = BeanDefinitionBuilder.rootBeanDefinition(JtaMonitor.class);
		jtaMonitorBuilder.addPropertyValue("uniqueName", element.getAttribute("unique-name"));
		final BeanDefinition jtaMonitor = jtaMonitorBuilder.getBeanDefinition();
		parserContext.getRegistry().registerBeanDefinition("spring4jtaMonitor", jtaMonitor);

		// JTA-TRANSACTION-MANAGER
		final BeanDefinitionBuilder jtaTransactionManagerBuilder = BeanDefinitionBuilder.rootBeanDefinition(JtaTransactionManager.class);
		final BeanDefinition jtaTransactionManager = jtaTransactionManagerBuilder.getBeanDefinition();
		parserContext.getRegistry().registerBeanDefinition("spring4jtaTransactionManager", jtaTransactionManager);

		// SPRING-JTA-TRANSACTION-MANAGER
		final BeanDefinitionBuilder springJtaTransactionManagerBuilder = BeanDefinitionBuilder.rootBeanDefinition(org.springframework.transaction.jta.JtaTransactionManager.class);
		springJtaTransactionManagerBuilder.addPropertyReference("transactionManager", "spring4jtaTransactionManager");
		return springJtaTransactionManagerBuilder.getBeanDefinition();
	}
}
