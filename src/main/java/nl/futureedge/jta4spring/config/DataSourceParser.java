package nl.futureedge.jta4spring.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import nl.futureedge.jta4spring.jdbc.XADataSourceWrapper;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that
 * parses an {@code data-source} element and creates a {@link BeanDefinition}
 * for an {@link XADataSourceWrapper}.
 */
public class DataSourceParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
		// DATA-SOURCE
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(XADataSourceWrapper.class);
		builder.addPropertyReference("xaDataSource", element.getAttribute("xa-data-source"));
		return builder.getBeanDefinition();
	}
}
