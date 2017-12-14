# plugin_qiniu

tale 博客系统七牛图片上传插件


## 打包

```bash
mvn clean assembly:assembly
```

打包后生成 `target/plugin_qiniu-jar-with-dependencies.jar`
 
将 `plugin_qiniu-jar-with-dependencies.jar` 重命名为 `plugin_qiniu.jar` 存储在 `tale/resources/plugins` 目录重启即可。