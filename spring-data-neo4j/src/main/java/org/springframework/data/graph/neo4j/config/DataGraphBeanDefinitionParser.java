package org.springframework.data.graph.neo4j.config;

import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.w3c.dom.Element;

import static org.springframework.util.StringUtils.hasText;

public class DataGraphBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String GRAPH_DATABASE_SERVICE = "graphDatabaseService";

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext context) {
        BeanDefinitionBuilder configBuilder = createConfigurationBeanDefinition();
        setupGraphDatabase(element, context, configBuilder);
        setupEntityManagerFactory(element, configBuilder);
        setupConfigurationClassPostProcessor(context);
        return getSourcedBeanDefinition(configBuilder, element, context);
    }

    private BeanDefinitionBuilder createConfigurationBeanDefinition() {
        BeanDefinitionBuilder configBuilder = BeanDefinitionBuilder.rootBeanDefinition(Neo4jConfiguration.class);
        configBuilder.setAutowireMode(Autowire.BY_TYPE.value());
        return configBuilder;
    }

    private void setupConfigurationClassPostProcessor(final ParserContext parserContext) {
        BeanDefinitionRegistry beanDefinitionRegistry = parserContext.getRegistry();

        BeanDefinitionBuilder configurationClassPostProcessor = BeanDefinitionBuilder.rootBeanDefinition(ConfigurationClassPostProcessor.class);
        BeanNameGenerator beanNameGenerator = parserContext.getReaderContext().getReader().getBeanNameGenerator();
        AbstractBeanDefinition configurationClassPostProcessorBeanDefinition = configurationClassPostProcessor.getBeanDefinition();
        String beanName = beanNameGenerator.generateBeanName(configurationClassPostProcessorBeanDefinition, beanDefinitionRegistry);
        beanDefinitionRegistry.registerBeanDefinition(beanName, configurationClassPostProcessorBeanDefinition);
    }

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    private void setupGraphDatabase(Element element, ParserContext context, BeanDefinitionBuilder configBuilder) {
        String graphDatabaseRef = element.getAttribute(GRAPH_DATABASE_SERVICE);
        if (!hasText(graphDatabaseRef)) {
            graphDatabaseRef = handleStoreDir(element, context, configBuilder);
        }
        configBuilder.addPropertyReference(GRAPH_DATABASE_SERVICE, graphDatabaseRef);
    }

    private void setupEntityManagerFactory(Element element, BeanDefinitionBuilder configBuilder) {
        String entityManagerFactory = element.getAttribute("entityManagerFactory");
        if (hasText(entityManagerFactory)) {
            configBuilder.addPropertyReference("entityManagerFactory", entityManagerFactory);
        }
    }

    private String handleStoreDir(Element element, ParserContext context, BeanDefinitionBuilder configBuilder) {
        String storeDir = element.getAttribute("storeDirectory");
        if (!hasText(storeDir)) return null;

        BeanDefinitionBuilder graphDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedGraphDatabase.class);
        graphDefinitionBuilder.addConstructorArgValue(storeDir);
        graphDefinitionBuilder.setScope("singleton");
        graphDefinitionBuilder.setDestroyMethodName("shutdown");
        context.getRegistry().registerBeanDefinition(GRAPH_DATABASE_SERVICE, graphDefinitionBuilder.getBeanDefinition());
        configBuilder.addPropertyReference(GRAPH_DATABASE_SERVICE, GRAPH_DATABASE_SERVICE);
        return GRAPH_DATABASE_SERVICE;
    }

    private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source, ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }




}