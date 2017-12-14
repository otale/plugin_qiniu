package com.tale.plugins;

import com.blade.kit.StringKit;

/**
 * @author biezhi
 * @date 2017/12/14
 */
public interface QiniuConst {

    String PLUGIN_KEY_BUCKET_NAME  = "plugin_qiniu_bucketname";
    String PLUGIN_KEY_OPERATORNAME = "plugin_qiniu_operatorname";
    String PLUGIN_KEY_OPERATORPWD  = "plugin_qiniu_operatorpwd";
    String PLUGIN_KEY_ACTIVE       = "plugin_qiniu_active";
    String ATTACH_URL              = "attach_url";
    String SAVE_LOG_ACTION         = "保存七牛设置";
    String ERROR_MSG               = "请确认配置完整";
    String UPLOAD_URI              = "/admin/attach/upload";
    String DELETE_URI              = "/admin/attach/delete";

}