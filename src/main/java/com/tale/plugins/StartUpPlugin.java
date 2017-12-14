package com.tale.plugins;

import com.blade.Blade;
import com.blade.event.BeanProcessor;
import com.blade.ioc.annotation.Bean;
import com.tale.init.TaleConst;
import com.tale.model.dto.PluginMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dongyuxiang on 03/03/2017.
 */
@Bean
public class StartUpPlugin implements BeanProcessor {

    private static final Logger log = LoggerFactory.getLogger(StartUpPlugin.class);

    @Override
    public void processor(Blade blade) {
        log.info("启动七牛插件");
        TaleConst.PLUGIN_MENUS.add(new PluginMenu("七牛云设置", "qiniu", null));
    }

}
