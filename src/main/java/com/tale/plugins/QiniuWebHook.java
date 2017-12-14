package com.tale.plugins;

import com.blade.ioc.annotation.Bean;
import com.blade.ioc.annotation.Inject;
import com.blade.mvc.hook.Signature;
import com.blade.mvc.hook.WebHook;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.multipart.FileItem;
import com.blade.mvc.ui.RestResponse;
import com.google.gson.Gson;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.tale.exception.TipException;
import com.tale.init.TaleConst;
import com.tale.model.dto.LogActions;
import com.tale.model.dto.Types;
import com.tale.model.entity.Attach;
import com.tale.model.entity.Logs;
import com.tale.model.entity.Users;
import com.tale.service.SiteService;
import com.tale.utils.TaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author biezhi
 * @date 2017/12/14
 */
@Bean
public class QiniuWebHook implements WebHook {

    private static final Logger log = LoggerFactory.getLogger(QiniuWebHook.class);

    @Inject
    private SiteService siteService;

    static Auth auth = null;

    static String bucket = null;

    static String upToken = null;

    static UploadManager uploadManager = null;

    static BucketManager bucketManager = null;

    @Override
    public boolean before(Signature signature) {
        boolean isActive = TaleConst.OPTIONS.getBoolean(QiniuConst.PLUGIN_KEY_ACTIVE, false);
        if (!isActive) {
            return true;
        } else {
            if (auth == null) {
                bucket = TaleConst.OPTIONS.getOrNull(QiniuConst.PLUGIN_KEY_BUCKET_NAME);
                String name = TaleConst.OPTIONS.getOrNull(QiniuConst.PLUGIN_KEY_OPERATORNAME);
                String pass = TaleConst.OPTIONS.getOrNull(QiniuConst.PLUGIN_KEY_OPERATORPWD);

                auth = Auth.create(name, pass);
                upToken = auth.uploadToken(bucket);
                //构造一个带指定Zone对象的配置类
                Configuration cfg = new Configuration(Zone.autoZone());
                uploadManager = new UploadManager(cfg);
                bucketManager = new BucketManager(auth, cfg);
            }
        }

        log.info("执行七牛插件");

        Request  request  = signature.request();
        Response response = signature.response();
        String   uri      = request.uri();

        // 拦截上传接口
        if ("/admin/attach/upload".equals(uri)) {
            Users                 users       = TaleUtils.getLoginUser();
            Integer               uid         = users.getUid();
            Map<String, FileItem> fileItemMap = request.fileItems();
            Collection<FileItem>  fileItems   = fileItemMap.values();
            try {
                List<String> errorFiles = fileItems.parallelStream()
                        .map(this::upload)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                response.json(RestResponse.ok(errorFiles));
            } catch (Exception e) {
                log.error("七牛上传失败", e);
                response.json(RestResponse.fail());
                return false;
            }
            return false;
        }

        // 删除接口
        if ("/admin/attach/delete".equals(uri)) {
            try {
                Users   users  = TaleUtils.getLoginUser();
                Integer id     = request.queryInt("id", 0);
                Attach  attach = new Attach().find(id);
                if (null == attach) {
                    response.json(RestResponse.fail("不存在该附件"));
                    return false;
                }
                // 删除文件
                bucketManager.delete(bucket, attach.getFkey().substring(1));
                new Attach().delete(id);
                siteService.cleanCache(Types.C_STATISTICS);

                new Logs(LogActions.DEL_ATTACH, attach.getFkey(), request.address(), users.getUid()).save();
                response.json(RestResponse.ok());
            } catch (Exception e) {
                String msg = "附件删除失败";
                if (e instanceof TipException) {
                    msg = e.getMessage();
                } else {
                    log.error(msg, e);
                }
                response.json(RestResponse.fail(msg));
            }
            return false;
        }
        return true;
    }

    private String upload(FileItem fileItem) {
        Users   users = TaleUtils.getLoginUser();
        Integer uid   = users.getUid();

        String fname = fileItem.getFileName();
        if (fileItem.getLength() / 1024 <= TaleConst.MAX_FILE_SIZE) {
            String fkey  = TaleUtils.getFileKey(fname);
            String ftype = fileItem.getContentType().contains("image") ? Types.IMAGE : Types.FILE;
            try {
                String filePath = TaleUtils.UP_DIR + fkey;
                Files.write(Paths.get(filePath), fileItem.getData());
                com.qiniu.http.Response result = uploadManager.put(filePath, fkey.substring(1), upToken);
                if (null != result) {
                    //上传到七牛,解析上传成功的结果
                    DefaultPutRet putRet = new Gson().fromJson(result.bodyString(), DefaultPutRet.class);
                    Attach        attach = new Attach();
                    attach.setFname(fname);
                    attach.setFkey(fkey);
                    attach.setFtype(ftype);
                    attach.setAuthor_id(uid);
                    attach.save();
                } else {
                    log.warn("上传文件 [{}] 失败", fname);
                }
                Files.delete(Paths.get(filePath));
            } catch (IOException e) {
                log.error("文件上传失败", e);
            }
        } else {
            return fname;
        }
        siteService.cleanCache(Types.C_STATISTICS);
        return null;
    }

}
