package com.tale.plugins;

import com.blade.annotation.Order;
import com.blade.config.BConfig;
import com.blade.context.WebContextListener;
import com.tale.dto.PluginMenu;
import com.tale.init.TaleConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

/**
 * Created by dongyuxiang on 03/03/2017.
 */
@Order(sort = 999)
public class StartUpPlugin implements WebContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartUpPlugin.class);

    @Override
    public void init(BConfig bConfig, ServletContext sec) {
        LOGGER.info("启动七牛插件");
        TaleConst.plugin_menus.add(new PluginMenu("七牛设置", "qiniu", null));
    }
}
