# 发布到 Maven Central 指南

本文档详细说明如何将 `knife4j-error-collector` 发布到 Maven Central。

## 前置条件

- [x] Maven Central 账号已注册
- [ ] GitHub namespace 已验证
- [ ] GPG 密钥已生成
- [ ] Maven settings.xml 已配置

---

## 步骤 1：验证 GitHub Namespace

1. 登录 https://central.sonatype.com/
2. 进入 Publishing Settings -> Namespace
3. 看到 Verification Key: `fnqnh1fvct`
4. 在 GitHub 创建公开仓库：https://github.com/new
   - 仓库名称：`fnqnh1fvct`
   - 选择 Public
5. 回到 Maven Central 点击 **Verify Namespace**

---

## 步骤 2：安装 GPG 并生成密钥

### macOS 安装 GPG

```bash
# 使用 Homebrew 安装
brew install gnupg

# 验证安装
gpg --version
```

### 生成 GPG 密钥

```bash
# 生成密钥（按提示操作）
gpg --gen-key

# 输入以下信息：
# - Real name: Zhang Longjun
# - Email: zljmails@163.com
# - Passphrase: 设置一个密码（记住它！）
```

### 查看并上传公钥

```bash
# 查看密钥 ID
gpg --list-keys --keyid-format SHORT

# 输出类似：
# pub   rsa3072/ABCD1234 2025-01-15 [SC] [expires: 2027-01-15]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid           [ultimate] Zhang Longjun <zljmails@163.com>
# sub   rsa3072/EFGH5678 2025-01-15 [E] [expires: 2027-01-15]

# 上传公钥到服务器（ABCD1234 是你的密钥 ID）
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234
gpg --keyserver keys.openpgp.org --send-keys ABCD1234
```

---

## 步骤 3：获取 Maven Central Token

1. 登录 https://central.sonatype.com/
2. 点击右上角头像 -> **View Account**
3. 找到 **User Token** 区域
4. 点击 **Generate User Token**
5. 复制生成的 username 和 password（这不是你的登录密码！）

---

## 步骤 4：配置 Maven settings.xml

编辑 `~/.m2/settings.xml`（如果不存在则创建）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">
    
    <servers>
        <!-- Maven Central 认证 -->
        <server>
            <id>central</id>
            <!-- 这里填写 Generate User Token 获取的 username -->
            <username>你的Token用户名</username>
            <!-- 这里填写 Generate User Token 获取的 password -->
            <password>你的Token密码</password>
        </server>
    </servers>
    
    <profiles>
        <profile>
            <id>release</id>
            <properties>
                <!-- GPG 密钥密码 -->
                <gpg.passphrase>你的GPG密码</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
    
    <activeProfiles>
        <activeProfile>release</activeProfile>
    </activeProfiles>
    
</settings>
```

---

## 步骤 5：执行发布

```bash
# 进入项目目录
cd knife4j-error-collector

# 清理并发布
mvn clean deploy

# 如果 GPG 签名有问题，可以尝试：
mvn clean deploy -Dgpg.passphrase=你的GPG密码
```

---

## 发布后验证

发布成功后，大约 10-30 分钟后可以在以下地址搜索到：

- https://central.sonatype.com/search?q=knife4j-error-collector
- https://search.maven.org/search?q=g:com.github.zhanglongjun

---

## 用户使用方式

发布成功后，用户可以直接在 pom.xml 中添加依赖：

**Spring Boot 3.x 项目：**

```xml
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-jakarta-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Spring Boot 2.x 项目：**

```xml
<dependency>
    <groupId>com.github.zhanglongjun</groupId>
    <artifactId>knife4j-error-collector-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 常见问题

### Q: GPG 签名失败

```bash
# 确保 GPG Agent 运行
gpgconf --launch gpg-agent

# 或者在命令行指定密码
mvn clean deploy -Dgpg.passphrase=你的密码
```

### Q: 401 Unauthorized

检查 `~/.m2/settings.xml` 中的 `<server>` 配置：
- `<id>` 必须是 `central`
- username/password 必须是 **Generate User Token** 获取的，不是登录密码

### Q: Namespace 未验证

确保在 GitHub 创建了名为 `fnqnh1fvct` 的公开仓库，然后点击 Verify。

### Q: 发布后搜索不到

Maven Central 同步需要时间，通常 10-30 分钟。如果超过 24 小时仍然搜不到，检查发布日志是否有错误。
