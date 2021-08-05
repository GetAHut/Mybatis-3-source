/*
 * Copyright 2010-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring.mapper;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by {@code basePackage}, {@code annotationClass}, or
 * {@code markerInterface}. If an {@code annotationClass} and/or {@code markerInterface} is specified, only the
 * specified types will be searched (searching for all interfaces will be disabled).
 * <p>
 * This functionality was previously a private class of {@link MapperScannerConfigurer}, but was broken out in version
 * 1.2.0.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see MapperFactoryBean
 * @since 1.2.0
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathMapperScanner.class);

  // Copy of FactoryBean#OBJECT_TYPE_ATTRIBUTE which was added in Spring 5.2
  static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

  private boolean addToConfig = true;

  private boolean lazyInitialization;

  private SqlSessionFactory sqlSessionFactory;

  private SqlSessionTemplate sqlSessionTemplate;

  private String sqlSessionTemplateBeanName;

  private String sqlSessionFactoryBeanName;

  private Class<? extends Annotation> annotationClass;

  private Class<?> markerInterface;

  private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;

  private String defaultScope;

  public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
    super(registry, false);
  }

  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * Set whether enable lazy initialization for mapper bean.
   * 设置是否对mapper bean延迟初始化 默认false
   * <p>
   * Default is {@code false}.
   * </p>
   *
   * @param lazyInitialization
   *          Set the @{code true} to enable
   * @since 2.0.2
   */
  public void setLazyInitialization(boolean lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
  }

  public void setMarkerInterface(Class<?> markerInterface) {
    this.markerInterface = markerInterface;
  }

  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateBeanName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
  }

  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryBeanName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
  }

  /**
   * @deprecated Since 2.0.1, Please use the {@link #setMapperFactoryBeanClass(Class)}.
   */
  @Deprecated
  public void setMapperFactoryBean(MapperFactoryBean<?> mapperFactoryBean) {
    this.mapperFactoryBeanClass = mapperFactoryBean == null ? MapperFactoryBean.class : mapperFactoryBean.getClass();
  }

  /**
   * Set the {@code MapperFactoryBean} class.
   *
   * 设置MapperFactoryBean.class
   *
   * @param mapperFactoryBeanClass
   *          the {@code MapperFactoryBean} class
   * @since 2.0.1
   */
  public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
  }

  /**
   * Set the default scope of scanned mappers.
   * 扫描Mapper范围
   * <p>
   * Default is {@code null} (equiv to singleton).
   * </p>
   *
   * @param defaultScope
   *          the scope
   * @since 2.0.6
   */
  public void setDefaultScope(String defaultScope) {
    this.defaultScope = defaultScope;
  }

  /**
   * Configures parent scanner to search for the right interfaces. It can search for all interfaces or just for those
   * that extends a markerInterface or/and those annotated with the annotationClass
   * 配置扫描器扫描正确的接口  被标记过的  或者有注解的
   */
  public void registerFilters() {
    //默认扫描所有Mapper接口
    boolean acceptAllInterfaces = true;

    // if specified, use the given annotation and / or marker interface
    if (this.annotationClass != null) {
      //将制定的注解类型添加 到拦截器中
      addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
      acceptAllInterfaces = false;
    }

    // override AssignableTypeFilter to ignore matches on the actual marker interface
    //覆盖 AssignableTypeFilter 以忽略实际标记界面上的匹配的接口
    if (this.markerInterface != null) {
      addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
        @Override
        protected boolean matchClassName(String className) {
          return false;
        }
      });
      acceptAllInterfaces = false;
    }

    if (acceptAllInterfaces) {
      // default include filter that accepts all classes
      //默认过滤器 扫描全接口
      addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

    // exclude package-info.java
    //package-info.java 用法参照 https://blog.csdn.net/u013084266/article/details/105948544/
    addExcludeFilter((metadataReader, metadataReaderFactory) -> {
      String className = metadataReader.getClassMetadata().getClassName();
      return className.endsWith("package-info");
    });
  }

  /**
   * Calls the parent search that will search and register all the candidates. Then the registered objects are post
   * processed to set them as MapperFactoryBeans
   * 重写了Spring中的doScan方法
   * 将Mapper.class 注册为 MapperFactoryBeans
   */
  @Override
  public Set<BeanDefinitionHolder> doScan(String... basePackages) {
    //调用父类ClassPathBeanDefinitionScanner进行扫描
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

    //mapper接口 不为空
    if (beanDefinitions.isEmpty()) {
      LOGGER.warn(() -> "No MyBatis mapper was found in '" + Arrays.toString(basePackages)
          + "' package. Please check your configuration.");
    } else {
      //mybatis做的偷天换日的操作， spring中bean只能为对象， 接口时不能实例化的。
      //处理Mybatis扫描到的BeanDefinition
      processBeanDefinitions(beanDefinitions);
    }

    return beanDefinitions;
  }

  /**
   * 处理beanDefinition
   * @param beanDefinitions
   */
  private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    AbstractBeanDefinition definition;
    BeanDefinitionRegistry registry = getRegistry();
    //循环处理所有beanDefinition
    for (BeanDefinitionHolder holder : beanDefinitions) {
      //get beanDefinition
      definition = (AbstractBeanDefinition) holder.getBeanDefinition();
      boolean scopedProxy = false;
      if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
        definition = (AbstractBeanDefinition) Optional
            .ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
            .map(BeanDefinitionHolder::getBeanDefinition).orElseThrow(() -> new IllegalStateException(
                "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder + "]"));
        scopedProxy = true;
      }
      //获取beanName
      String beanClassName = definition.getBeanClassName();
      LOGGER.debug(() -> "Creating MapperFactoryBean with name '" + holder.getBeanName() + "' and '" + beanClassName
          + "' mapperInterface");

      // the mapper interface is the original class of the bean
      // but, the actual class of the bean is MapperFactoryBean
      //Mapper.class是初始bean，但实际bean是MapperFactoryBean<Mapper.class>
      //通过构造器传参给MapperFactoryBean传入mapper.class name
      definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
      //设置成MapperFactoryBean
      definition.setBeanClass(this.mapperFactoryBeanClass);

      definition.getPropertyValues().add("addToConfig", this.addToConfig);

      // Attribute for MockitoPostProcessor
      // https://github.com/mybatis/spring-boot-starter/issues/475
      definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClassName);

      boolean explicitFactoryUsed = false;
      if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
        definition.getPropertyValues().add("sqlSessionFactory",
            new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionFactory != null) {
        definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
        explicitFactoryUsed = true;
      }

      if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
        if (explicitFactoryUsed) {
          LOGGER.warn(
              () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate",
            new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionTemplate != null) {
        if (explicitFactoryUsed) {
          LOGGER.warn(
              () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
        explicitFactoryUsed = true;
      }

      if (!explicitFactoryUsed) {
        LOGGER.debug(() -> "Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
        //factoryBean 中依旧需要SqlSessionFactory ， SqlSessionFactory是已经注册在beanDefinition中了 设置自动注入方式 即可。
        //mybatis在此处有一个小细节， 在配置SqlSessionFactoryBean的时候 将SqlSessionFactoryBean转换为了SqlSessionFactory 所有可以直接注入
        //自动注入方式有两种 set方法注入 和构造器注入
        //自动装配模型有5中  不自动装备， byName byType byConstructor autodetect
        //autodetect 如果有默认的constructor 则使用构造器装备， 如果没有在使用byType
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
      }

      definition.setLazyInit(lazyInitialization);

      if (scopedProxy) {
        continue;
      }

      if (ConfigurableBeanFactory.SCOPE_SINGLETON.equals(definition.getScope()) && defaultScope != null) {
        definition.setScope(defaultScope);
      }

      if (!definition.isSingleton()) {
        BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
        if (registry.containsBeanDefinition(proxyHolder.getBeanName())) {
          registry.removeBeanDefinition(proxyHolder.getBeanName());
        }
        //bean注册
        registry.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
      }

    }
  }

  /**
   * {@inheritDoc}
   * 重写了Spring扫描器中调用doScan()时判断bean的方法， 将接口扫描进去。
   */
  @Override
  protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
    if (super.checkCandidate(beanName, beanDefinition)) {
      return true;
    } else {
      LOGGER.warn(() -> "Skipping MapperFactoryBean with name '" + beanName + "' and '"
          + beanDefinition.getBeanClassName() + "' mapperInterface" + ". Bean already defined with the same name!");
      return false;
    }
  }

}
