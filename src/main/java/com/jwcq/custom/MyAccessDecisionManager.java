package com.jwcq.custom;

import com.jwcq.global.result.Response;
import com.jwcq.utils.JsonUtils;
import com.jwcq.utils.SystemConfig;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

/**
 * Created by luotuo on 17-7-3.
 */
@Service
public class MyAccessDecisionManager implements AccessDecisionManager {

    //decide 方法是判定是否拥有权限的决策方法
    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        HttpServletRequest request = ((FilterInvocation) object).getHttpRequest();
        HttpServletResponse response = ((FilterInvocation) object).getResponse();
        String url, method;
        AntPathRequestMatcher matcher;
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (ga instanceof AuthorityInfo) {
                AuthorityInfo urlGrantedAuthority = (AuthorityInfo) ga;
                url = urlGrantedAuthority.getUrl();
                method = urlGrantedAuthority.getMethod();
                matcher = new AntPathRequestMatcher(url);
                if (matcher.matches(request)) {
                    //当权限表权限的method为ALL时表示拥有此路径的所有请求方式权利。
//                    if (method.equals(request.getMethod())) {
//                        return;
//                    }
                    return;
                }
            } else if (ga.getAuthority().equals("ROLE_ANONYMOUS")) {//未登录只允许访问 login 页面
                matcher = new AntPathRequestMatcher("/login");
                if (matcher.matches(request)) {
                    return;
                }
                break;
            }
        }
        // Tell web that you have no right!
        //throw new AccessDeniedException("no right");
        System.out.println("request.getRequestURI() == " + request.getRequestURI());
        if (request.getRequestURI().equals("/3audit/user/currentUserInfo") ||
                request.getRequestURI().equals("/3audit/user/join") ||
                request.getRequestURI().equals("/3audit/publish/task/editAllowAll") ||
                request.getRequestURI().equals("/3audit/publish/task/getQRCodeUrl") ||
                request.getRequestURI().equals("/3audit/user/bindWechat") ||
                request.getRequestURI().equals("/3audit/user/wechatJoin"))
            return;
        buildAFailureResponse(response, request, authentication);
        throw new AccessDeniedException("no right");
    }

    private HttpServletResponse buildAFailureResponse(HttpServletResponse response,
                                                      HttpServletRequest request,
                                                      Authentication authentication) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "X-Request-With, JWCQ, Origin,Content-Type");
        response.setContentType("text/plain;charset='utf-8'");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);

        // Our ajax request
        Response response1 = new Response();
        response1.setSuccess(1);
        String resStr = "没有权限";

        if (authentication.getPrincipal().toString().equals("anonymousUser")) {
            resStr = SystemConfig.getProperty("cas.server.host.url")+"/login?service=";
            try {
                String referer = URLDecoder.decode(request.getHeader("referer"), "UTF-8");
                if (referer.contains("/html/"))
                    resStr += URLEncoder.encode(SystemConfig.getProperty("app.server.host.url") + "/login?platform=task", "UTF-8");
                else
                    resStr += URLEncoder.encode(SystemConfig.getProperty("app.server.host.url") + "/login?platform=project", "UTF-8");
            } catch (Exception e) {

            }
            response.setStatus(203);
        }
        response1.setMessage(resStr);
        response1.setResult(resStr);
        String responseStr = "";
        try {
            responseStr = JsonUtils.writeEntityJSON(response1);
            PrintWriter out = response.getWriter();
            out.append(responseStr);
            out.close();
        } catch (IOException ioe) {
            // FIXME: Add log here!
        }
        return response;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }
}