package com.tale.plugins;


import com.blade.ioc.annotation.Inject;
import com.blade.kit.JsonKit;
import com.blade.kit.StringKit;
import com.blade.mvc.annotation.JSON;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.annotation.Route;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.Request;
import com.blade.mvc.ui.RestResponse;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.tale.controller.BaseController;
import com.tale.exception.TipException;
import com.tale.extension.Commons;
import com.tale.init.TaleConst;
import com.tale.model.entity.Logs;
import com.tale.service.OptionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by dongyuxiang on 03/03/2017.
 */
@Path("admin/plugins/qiniu")
public class QiniuController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(QiniuController.class);

    @Inject
    private OptionsService optionsService;

    @Route(value = "", method = HttpMethod.GET)
    public String index(Request request) {
        Map<String, String> options = optionsService.getOptions();
        request.attribute("options", options);
        return "plugins/qiniu";
    }

    /**
     * 保存七牛设置
     *
     * @return
     */
    @Route(value = "save", method = HttpMethod.POST)
    @JSON
    public RestResponse save(Request request) {
        try {
            // 空间名
            String bucket = request.query(QiniuConst.PLUGIN_KEY_BUCKET_NAME, "");
            // 操作员名
            String name = request.query(QiniuConst.PLUGIN_KEY_OPERATORNAME, "");
            // 操作密码
            String pass = request.query(QiniuConst.PLUGIN_KEY_OPERATORPWD, "");
            // 是否开启插件
            String active = request.query(QiniuConst.PLUGIN_KEY_ACTIVE, "");
            // 域名
            String attach_url = request.query(QiniuConst.ATTACH_URL, "");

            if ("true".equals(active)) {
                if (StringKit.isBlank(bucket) || StringKit.isBlank(name) || StringKit.isBlank(pass) ||
                        StringKit.isBlank(active) || StringKit.isBlank(attach_url)) {
                    return RestResponse.fail(QiniuConst.ERROR_MSG);
                }
                if (attach_url.endsWith("/")) {
                    attach_url = attach_url.substring(0, attach_url.length() - 1);
                }
                if (!attach_url.startsWith("http")) {
                    attach_url = "http://" + attach_url;
                }
            } else {
                attach_url = Commons.site_url();
            }

            optionsService.saveOption(QiniuConst.PLUGIN_KEY_BUCKET_NAME, bucket);
            optionsService.saveOption(QiniuConst.PLUGIN_KEY_OPERATORNAME, name);
            optionsService.saveOption(QiniuConst.PLUGIN_KEY_OPERATORPWD, pass);
            optionsService.saveOption(QiniuConst.PLUGIN_KEY_ACTIVE, active);
            optionsService.saveOption(QiniuConst.ATTACH_URL, attach_url);

            TaleConst.OPTIONS.addAll(optionsService.getOptions());

            if (StringKit.isNotBlank(name) && StringKit.isNotBlank(pass) && StringKit.isNotBlank(bucket)) {
                QiniuWebHook.auth = Auth.create(name, pass);
                QiniuWebHook.upToken = QiniuWebHook.auth.uploadToken(bucket);
                //构造一个带指定Zone对象的配置类
                Configuration cfg = new Configuration(Zone.autoZone());
                QiniuWebHook.uploadManager = new UploadManager(cfg);
                QiniuWebHook.bucketManager = new BucketManager(QiniuWebHook.auth, cfg);
            }

            new Logs(QiniuConst.SAVE_LOG_ACTION, JsonKit.toString(request.parameters()), request.address(), this.getUid()).save();
            return RestResponse.ok();
        } catch (Exception e) {
            String msg = "保存设置失败";
            if (e instanceof TipException) {
                msg = e.getMessage();
            } else {
                log.error(msg, e);
            }
            return RestResponse.fail(msg);
        }
    }
}
