package nl.futureedge.jta4spring.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import nl.futureedge.jta4spring.jms.XAConnectionFactoryWrapper;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that
 * parses an {@code connection-factory} element and creates a {@link BeanDefinition}
 * for an {@link XAConnectionFactoryWrapper}.
 */
public class ConnectionFactoryParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
		// CONNECTION-FACTORY
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(XAConnectionFactoryWrapper.class);
		builder.addPropertyReference("xaConnectionFactory", element.getAttribute("xa-connection-factory"));
		return builder.getBeanDefinition();
	}
}
