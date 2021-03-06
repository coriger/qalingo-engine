/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.7.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2013
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package org.hoteia.qalingo.core.web.servlet;

import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.wurfl.core.Device;
import net.sourceforge.wurfl.core.WURFLHolder;
import net.sourceforge.wurfl.core.WURFLManager;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hoteia.qalingo.core.domain.Market;
import org.hoteia.qalingo.core.domain.MarketArea;
import org.hoteia.qalingo.core.domain.MarketPlace;
import org.hoteia.qalingo.core.pojo.RequestData;
import org.hoteia.qalingo.core.web.util.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.View;

public class DispatcherServlet extends org.springframework.web.servlet.DispatcherServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Generated UID
     */
    private static final long serialVersionUID = 2588762975032561784L;

    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        initPlatformTheme(request);
        initPlatformDevice(request);
        super.doDispatch(request, response);
    }

    @Override
    protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {
        View view = super.resolveViewName(viewName, model, locale, request);
        if (view == null) {
            String fullViewNameSplit[] = viewName.split("/");
            if (fullViewNameSplit.length > 1) {
                String theme = fullViewNameSplit[1];
                String deviceDefaultViewName = "/" + theme + "/www/default";
                for (int i = 4; i < fullViewNameSplit.length; i++) {
                    String string = fullViewNameSplit[i];
                    deviceDefaultViewName = deviceDefaultViewName + "/" + string;
                }
                view = super.resolveViewName(deviceDefaultViewName, model, locale, request);
            }
            if (view == null) {
                String fullDefaultViewName = "/default/www/default";
                for (int i = 4; i < fullViewNameSplit.length; i++) {
                    String string = fullViewNameSplit[i];
                    fullDefaultViewName = fullDefaultViewName + "/" + string;
                }
                view = super.resolveViewName(fullDefaultViewName, model, locale, request);
            }
        }
        return view;
    }

    private void initPlatformTheme(HttpServletRequest request) {
        final ServletContext context = getServletContext();
        final ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
        final RequestUtil requestUtil = (RequestUtil) ctx.getBean("requestUtil");

        // THEME
        try {
            final RequestData requestData = requestUtil.getRequestData(request);
            final MarketArea marketArea = requestData.getMarketArea();
            if (marketArea != null && StringUtils.isNotEmpty(marketArea.getTheme())) {
                String themeFolder = marketArea.getTheme();
                requestUtil.updateCurrentTheme(request, themeFolder);
            } else {
                final Market market = requestData.getMarket();
                if (market != null && StringUtils.isNotEmpty(market.getTheme())) {
                    String themeFolder = market.getTheme();
                    requestUtil.updateCurrentTheme(request, themeFolder);
                } else {
                    final MarketPlace marketPlace = requestData.getMarketPlace();
                    if (marketPlace != null && StringUtils.isNotEmpty(marketPlace.getTheme())) {
                        String themeFolder = marketPlace.getTheme();
                        requestUtil.updateCurrentTheme(request, themeFolder);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void initPlatformDevice(HttpServletRequest request) {
        final ServletContext context = getServletContext();
        final ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
        final RequestUtil requestUtil = (RequestUtil) ctx.getBean("requestUtil");

        // DEVICE
        try {
            final RequestData requestData = requestUtil.getRequestData(request);

            final WURFLHolder wurfl = (WURFLHolder) ctx.getBean("wurflHolder");
            final WURFLManager manager = wurfl.getWURFLManager();
            Device device = manager.getDeviceForRequest(request);
            String deviceFolder = "default";
            if (device != null) {
                boolean isSmartPhone = BooleanUtils.toBoolean(device.getVirtualCapability("is_smartphone"));
                boolean isIPhoneOs = BooleanUtils.toBoolean(device.getVirtualCapability("is_iphone_os"));
                boolean isAndroid = BooleanUtils.toBoolean(device.getVirtualCapability("is_android"));
                if (isSmartPhone || isIPhoneOs || isAndroid) {
                    deviceFolder = "mobile";
                }
            }
            requestUtil.updateCurrentDevice(requestData, deviceFolder);

        } catch (Exception e) {
            logger.error("", e);
        }
    }

}