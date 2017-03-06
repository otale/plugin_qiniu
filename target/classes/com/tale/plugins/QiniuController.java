package com.tale.plugins;


import com.blade.ioc.annotation.Inject;
import com.blade.kit.IPKit;
import com.blade.kit.StringKit;
import com.blade.kit.base.Config;
import com.blade.kit.json.JSONKit;
import com.blade.mvc.annotation.Controller;
import com.blade.mvc.annotation.JSON;
import com.blade.mvc.annotation.Route;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.Request;
import com.blade.mvc.view.RestResponse;
import com.qiniu.util.Auth;
import com.tale.controller.BaseController;
import com.tale.exception.TipException;
import com.tale.ext.Commons;
import com.tale.init.TaleConst;
import com.tale.service.LogService;
import com.tale.service.OptionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by dongyuxiang on 03/03/2017.
 */
@Controller("admin/plugins/qiniu")
public class QiniuController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QiniuController.class);

    @Inject
    private OptionsService optionsService;

    @Inject
    private LogService logService;

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
            String bucket = request.query("plugin_qiniu_bucketname");
            // 操作员名
            String name = request.query("plugin_qiniu_operatorname");
            // 操作密码
            String pass = request.query("plugin_qiniu_operatorpwd");
            // 是否开启插件
            String active = request.query("plugin_qiniu_active");
            // 域名
            String attach_url = request.query("attach_url");

            if ("true".equals(active)) {
                if (StringKit.isBlank(bucket) || StringKit.isBlank(name) || StringKit.isBlank(pass) ||
                        StringKit.isBlank(active) || StringKit.isBlank(attach_url)) {
                    return RestResponse.fail("请确认配置完整");
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

            Map<String, String> querys = request.querys();

            optionsService.saveOption("plugin_qiniu_bucketname", bucket);
            optionsService.saveOption("plugin_qiniu_operatorname", name);
            optionsService.saveOption("plugin_qiniu_operatorpwd", pass);
            optionsService.saveOption("plugin_qiniu_active", active);
            optionsService.saveOption("attach_url", attach_url);

            Config config = new Config();
            config.addAll(optionsService.getOptions());
            TaleConst.OPTIONS = config;

            QiniuPlugin.auth = Auth.create(name, pass);
            QiniuPlugin.upToken = QiniuPlugin.auth.uploadToken(bucket);
            logService.save("保存七牛设置", JSONKit.toJSONString(querys), IPKit.getIpAddrByRequest(request.raw()), this.getUid());
            return RestResponse.ok();
        } catch (Exception e) {
            String msg = "保存设置失败";
            if (e instanceof TipException) {
                msg = e.getMessage();
            } else {
                LOGGER.error(msg, e);
            }
            return RestResponse.fail(msg);
        }
    }
}
