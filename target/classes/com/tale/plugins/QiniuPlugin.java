package com.tale.plugins;


import com.blade.ioc.annotation.Inject;
import com.blade.mvc.annotation.Intercept;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.interceptor.Interceptor;
import com.blade.mvc.multipart.FileItem;
import com.blade.mvc.view.RestResponse;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.tale.dto.LogActions;
import com.tale.dto.Types;
import com.tale.exception.TipException;
import com.tale.init.TaleConst;
import com.tale.model.Attach;
import com.tale.model.Users;
import com.tale.service.AttachService;
import com.tale.service.LogService;
import com.tale.service.SiteService;
import com.tale.utils.TaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by dongyuxiang on 03/03/2017.
 */
@Intercept(value = "/admin/.*")
public class QiniuPlugin implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QiniuPlugin.class);

    @Inject
    private AttachService attachService;

    @Inject
    private SiteService siteService;

    @Inject
    private LogService logService;

    static Auth auth = null;

    static String bucket = null;

    static String upToken  = null;

    static UploadManager uploadManager = null;

    static BucketManager bucketManager = null;

    @Override
    public boolean before(Request request, Response response) {

        boolean isActive = TaleConst.OPTIONS.getBoolean("plugin_qiniu_active", false);
        if (!isActive) {
            return true;
        } else {
            if (auth == null) {
                bucket = TaleConst.OPTIONS.get("plugin_qiniu_bucketname");
                String name = TaleConst.OPTIONS.get("plugin_qiniu_operatorname");
                String pass = TaleConst.OPTIONS.get("plugin_qiniu_operatorpwd");

                auth = Auth.create(name, pass);
                upToken = auth.uploadToken(bucket);
                //构造一个带指定Zone对象的配置类
                Configuration cfg = new Configuration(Zone.autoZone());
                uploadManager = new UploadManager(cfg);
                bucketManager = new BucketManager(auth, cfg);
            }
        }

        LOGGER.info("执行七牛插件");

        String uri = request.uri();

        // 拦截上传接口
        if ("/admin/attach/upload".equals(uri)) {
            Users users = TaleUtils.getLoginUser();
            Integer uid = users.getUid();
            Map<String, FileItem> fileItemMap = request.fileItems();
            Collection<FileItem> fileItems = fileItemMap.values();
            try {
                List<String> errorFiles = new ArrayList<>();
                fileItems.forEach(f -> {
                    String fname = f.fileName();
                    if (f.file().length() / 1024 <= TaleConst.MAX_FILE_SIZE) {
                        String fkey = TaleUtils.getFileKey(fname);
                        String ftype = TaleUtils.isImage(f.file()) ? Types.IMAGE : Types.FILE;
                        try {
                            com.qiniu.http.Response result = uploadManager.put(f.file(), fkey.substring(1), upToken);
                            if(null != result){
                                //上传到七牛,解析上传成功的结果
                                DefaultPutRet putRet = new Gson().fromJson(result.bodyString(), DefaultPutRet.class);
                                attachService.save(putRet.hash, fkey, ftype, uid);
                            }else{
                                LOGGER.warn("上传文件 [{}] 失败", f.fileName());
                            }
                            f.file().delete();
                        } catch (QiniuException e) {
                            e.printStackTrace();
                        }
                    } else {
                        errorFiles.add(fname);
                    }
                    siteService.cleanCache(Types.C_STATISTICS);
                });
                response.json(RestResponse.ok(errorFiles));
            } catch (Exception e) {
                LOGGER.error("七牛上传失败", e);
                response.json(RestResponse.fail());
                return false;
            }
            return false;
        }

        // 删除接口
        if ("/admin/attach/delete".equals(uri)) {
            try {
                Users users = TaleUtils.getLoginUser();
                Integer id = request.queryInt("id");
                Attach attach = attachService.byId(id);
                if (null == attach) {
                    response.json(RestResponse.fail("不存在该附件"));
                    return false;
                }
                // 删除文件
                bucketManager.delete(bucket, attach.getFkey().substring(1));
                attachService.delete(id);
                siteService.cleanCache(Types.C_STATISTICS);
                logService.save(LogActions.DEL_ARTICLE, attach.getFkey(), request.address(), users.getUid());
                response.json(RestResponse.ok());
            } catch (Exception e) {
                String msg = "附件删除失败";
                if (e instanceof TipException) {
                    msg = e.getMessage();
                } else {
                    LOGGER.error(msg, e);
                }
                response.json(RestResponse.fail(msg));
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean after(Request request, Response response) {
        return true;
    }

}
