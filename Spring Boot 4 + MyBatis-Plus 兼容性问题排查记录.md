# Spring Boot 4.0 + MyBatis-Plus 3.5.12 兼容性问题排查记录

## 项目环境

| 项目 | 版本/信息 |
|------|-----------|
| Spring Boot | 4.0.0 |
| Java | 21.0.7 |
| MyBatis-Plus | 3.5.12 (`mybatis-plus-spring-boot3-starter`) |
| MyBatis Starter | 4.0.1 (`mybatis-spring-boot-starter`) |
| 数据库 | MySQL 8.x |
| 连接池 | HikariCP（Spring Boot 内置） |
| 模板引擎 | Thymeleaf |
| 服务端口 | 8080 |

---

## 问题现象

### 现象 1：Mapper 重复扫描后全部丢失

启动日志出现大量 WARN：

```
WARN  o.m.s.mapper.ClassPathMapperScanner : Skipping MapperFactoryBean with name 'BBSCategoryMapper'
      and 'com.bbs.bbs.Mapper.BBSCategoryMapper' mapperInterface. Bean already defined with the same name!

WARN  o.m.s.mapper.ClassPathMapperScanner : Skipping MapperFactoryBean with name 'BBSCollectMapper' ...
WARN  o.m.s.mapper.ClassPathMapperScanner : Skipping MapperFactoryBean with name 'BBSCommentMapper' ...
WARN  o.m.s.mapper.ClassPathMapperScanner : Skipping MapperFactoryBean with name 'BBSPostMapper' ...
WARN  o.m.s.mapper.ClassPathMapperScanner : Skipping MapperFactoryBean with name 'BBSUserMapper' ...

WARN  o.m.s.mapper.ClassPathMapperScanner : No MyBatis mapper was found in '[com.bbs.bbs.Mapper]' package.
      Please check your configuration.
```

**结果**：所有 Mapper 先被注册，然后因重复被跳过，最终「找不到任何 Mapper」。

### 现象 2：MyBatis-Plus 自动配置未生效

启动日志中 **没有** 出现以下标志性输出：

```
 _ _   |_  _ _|_. ___ _ |    _
| | |\/|_)(_| | |_\  |_)||_|_\
     /               |
                        3.5.12
```

通过 `--debug` 启动参数查看自动配置报告，确认：

```
MybatisPlusAutoConfiguration:
   Did not match:
      - @ConditionalOnSingleCandidate (types: javax.sql.DataSource; SearchStrategy: all)
        did not find any beans (OnBeanCondition)
   Matched:
      - @ConditionalOnClass found required classes 'org.apache.ibatis.session.SqlSessionFactory',
        'org.mybatis.spring.SqlSessionFactoryBean' (OnClassCondition)
```

**MyBatis-Plus 的类被找到了（Class 条件通过），但 DataSource Bean 条件失败了。**

### 现象 3：HikariCP 没有启动日志

正常情况下应该看到：

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

但控制台完全没有。

### 现象 4：排除 MybatisAutoConfiguration 后报错

尝试排除官方 MyBatis 自动配置后：

```java
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
```

立即报错：

```
Error creating bean with name 'BBSCategoryMapper' defined in file
[D:\JavaWorkSpace\BBS\target\classes\com\bbs\bbs\Mapper\BBSCategoryMapper.class]:
Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required
```

---

## 根因分析

### 核心矛盾：自动配置加载顺序问题

这是 **Spring Boot 4.0** 与 **MyBatis-Plus 3.5.12** 之间的兼容性问题。

#### 正常流程（Spring Boot 3.x）

```
1. DataSourceAutoConfiguration → 创建 HikariCP DataSource Bean
2. MybatisPlusAutoConfiguration → 检测到 DataSource，创建 SqlSessionFactory
3. MapperScan → 注册 Mapper，绑定 SqlSessionFactory
```

#### 实际流程（Spring Boot 4.0）

```
1. MybatisPlusAutoConfiguration 被提前评估
   → 此时 DataSource Bean 还未创建
   → @ConditionalOnSingleCandidate(DataSource.class) 失败
   → MybatisPlusAutoConfiguration 被跳过 ❌

2. MybatisAutoConfiguration 抢先匹配
   → 创建了原生 MyBatis 的 SqlSessionFactory（不包含 MyBatis-Plus 增强功能）

3. DataSourceAutoConfiguration → 创建 HikariCP DataSource Bean（实际已创建，但来得太晚）

4. Mapper 被扫描两次（@MapperScan + 自动扫描）→ 重复后被跳过
```

#### 为什么会这样？

Spring Boot 4.0 对自动配置类的**包路径进行了重大重构**：

| 组件 | Spring Boot 3.x 路径 | Spring Boot 4.0 路径 |
|------|----------------------|---------------------|
| DataSourceAutoConfiguration | `org.springframework.boot.autoconfigure.jdbc` | `org.springframework.boot.jdbc.autoconfigure` |

MyBatis-Plus 3.5.12 的 `MybatisPlusAutoConfiguration` 使用了 `@AutoConfigureAfter` 注解指向**旧路径**的 `DataSourceAutoConfiguration`：

```java
// MyBatis-Plus 源码中（3.5.12）
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
// ↑ 引用的是旧包路径，Spring Boot 4 找不到，排序失效！
```

Spring Boot 4 找不到对应的旧路径类，**排序约束失效**，导致 `MybatisPlusAutoConfiguration` 可能在 `DataSourceAutoConfiguration` 之前被评估。

### Mapper 重复扫描的原因

1. `@MapperScan("com.bbs.bbs.Mapper")` 注册了一次
2. `MybatisAutoConfiguration.MapperScannerRegistrarNotFoundConfiguration` 又自动注册了一次
3. 第二次注册时发现 Bean 已存在 → 打印 `Skipping` 警告
4. 但最终结果是 `No MyBatis mapper was found`

---

## 解决方案

### 已尝试但失败的方案

#### 方案 A：排除 MybatisAutoConfiguration

```java
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
```

**结果**：`MybatisPlusAutoConfiguration` 仍然因排序问题不生效，没人创建 `SqlSessionFactory`，报错 `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`。

### 可行的解决方案

#### 方案 1：降级 Spring Boot 到 3.x（推荐，最稳妥）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.4</version>  <!-- 降回 3.x -->
    <relativePath/>
</parent>
```

**理由**：MyBatis-Plus 3.5.12 官方只支持到 Spring Boot 3.x，Spring Boot 4.0 兼容性尚未被官方验证。

#### 方案 2：手动配置 SqlSessionFactory

在 `MybatisPlusConfig.java` 中手动创建 MyBatis-Plus 的 `SqlSessionFactory`，绕过自动配置：

```java
@Configuration
@MapperScan("com.bbs.bbs.Mapper")
public class MybatisPlusConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*Mapper.xml")
        );
        // 添加分页插件等
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factoryBean.setPlugins(interceptor);
        return factoryBean.getObject();
    }
}
```

同时在主类排除冲突的自动配置：

```java
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
```

#### 方案 3：等待 MyBatis-Plus 发布 Spring Boot 4 兼容版本

关注 MyBatis-Plus 官方仓库，等待发布适配 Spring Boot 4.0 的新版本（预计会更新 `@AutoConfigureAfter` 的包路径引用）。

---

## HikariCP 未显示启动日志的说明

根据自动配置报告的 **Positive matches**，`DataSourceConfiguration.Hikari` 实际上**已经匹配成功**：

```
DataSourceConfiguration.Hikari matched:
   - @ConditionalOnClass found required class 'com.zaxxer.hikari.HikariDataSource' (OnClassCondition)
   - @ConditionalOnProperty (spring.datasource.type=com.zaxxer.hikari.HikariDataSource) matched
   - @ConditionalOnMissingBean (types: javax.sql.DataSource) did not find any beans
```

HikariCP 的连接池是**懒初始化**的——只有在第一次执行数据库操作时才会真正建立连接并打印：

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

如果应用启动后没有任何请求访问数据库，就不会看到这些日志。发送一个数据库请求后即可看到。

---

## 关键教训

1. **Spring Boot 大版本升级要谨慎**：4.0 对自动配置类的包路径进行了大规模迁移，第三方 starter 可能还没适配。
2. **查看自动配置报告是排查利器**：启动参数加 `--debug` 可以看到所有自动配置的匹配/不匹配原因。
3. **MyBatis 和 MyBatis-Plus 会冲突**：两者的自动配置类都会尝试创建 `SqlSessionFactory`，需要明确排除其中一个。
4. **Mapper 重复扫描**：`@MapperScan` 和自动配置的 `MapperScannerRegistrar` 会导致双重扫描，注意避免。

---

## 参考

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki)
- [MyBatis-Plus GitHub](https://github.com/baomidou/mybatis-plus)
- [MyBatis Spring Boot Starter 4.0.1](https://github.com/mybatis/spring-boot-starter)
- Spring Boot `--debug` 自动配置报告
