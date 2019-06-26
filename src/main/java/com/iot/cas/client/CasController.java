package com.iot.cas.client;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

@Controller
@RequestMapping("/cas")
public class CasController {
    
    Logger logger = LoggerFactory.getLogger(CasController.class);
    //登录状态session，模拟登录状态
    public static final HashMap<String, String> LOGIN_SESSION = new HashMap<>();

    /**
     * 单点登录服务地址
     */
    private static final String casUrl = "https://www.sso.com";
    /***
     * 当前服务地址
     */
    private static final String serviceUrl = "http://172.16.104.18:8088";


    /***
     * 登录地址
     * @param response HttpServletResponse
     * @throws IOException
     */
    @GetMapping("/login")
    public void redirect(HttpServletResponse response) throws IOException {
        //跳转单点登录服务地址
        response.sendRedirect(casUrl + "/cas/login?service=" + serviceUrl + "/cas/vailedST");
    }

    /***
     * 校验serviceTicket接口
     * @param serviceTicket 单点登录系统返回的serviceTicket
     * @param response HttpServletResponse
     * @throws DocumentException dom4j匹配xml错误
     */
    @GetMapping("/vailedST")
    @ResponseBody
    public String redirect(@RequestParam("ticket") String serviceTicket, HttpServletResponse response) throws DocumentException {
        logger.info("我接收到了ST:" + serviceTicket);
        //拼装st校验请求地址
        String url = casUrl + "/cas/serviceValidate?service=" + serviceUrl + "/cas/vailedST&ticket=" + serviceTicket;
        RestTemplate restTemplate = new RestTemplate();
        //发送请求
        ResponseEntity<String> entity = restTemplate.getForEntity(url, String.class);
        //判断请求是否成功
        if (entity.getStatusCode() == HttpStatus.OK) {
            /***
             * 请求响应格式如下：
             * <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
             *     <cas:authenticationSuccess>
             *         <cas:user>15</cas:user>
             *         </cas:authenticationSuccess>
             * </cas:serviceResponse>
             */
            String xmlStr = entity.getBody();
            logger.info(xmlStr);
            Document document = DocumentHelper.parseText(xmlStr);
            //使用dom4j取得cas:user处的用户id
            Node node = document.selectSingleNode("//cas:user");
            String idStr = node.getText();
            logger.info("登录用户的id为{}", idStr);
            //模拟服务器生成用户session
            LOGIN_SESSION.put(serviceTicket, idStr);
            return "id为"+idStr+"用户登录成功";
        }else {
            return "登录失败";
        }
    }

    /***
     * 登出地址
     * @param response HttpServletResponse
     * @throws IOException HttpServletResponse重定向异常
     */
    @GetMapping("/logout")
    public void casLogout(HttpServletResponse response) throws IOException {
        //跳转到单点登录系统的登出地址
        response.sendRedirect(casUrl + "/cas/logout");
    }

    /***
     * 提供给单点登录服务的单点登出地址
     * @param logoutRequest 登出参数
     * @param response HttpServletResponse
     * @throws DocumentException dom4j匹配xml错误
     */
    @PostMapping("/ssout")
    public void logoutPost(@ModelAttribute(name = "logoutRequest") String logoutRequest, HttpServletResponse response) throws DocumentException {
        logger.info(logoutRequest);
        /***
         * logoutRequest中格式如下：
         * <samlp:LogoutRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID="LR-1096-kEbgn1yTEP5obuTjgdoNu55w" Version="2.0" IssueInstant="2019-06-26T16:11:54Z">
         *   <saml:NameID xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">@NOT_USED@</saml:NameID>
         *   <samlp:SessionIndex>ST-1347-6KCWg4F4siZF-JL5OGnTfERYo8M-localhost</samlp:SessionIndex>
         * </samlp:LogoutRequest>
         */
        Document document = DocumentHelper.parseText(logoutRequest);
        //取得samlp:SessionIndex节点中的ST值
        Node stNode = document.selectSingleNode("//samlp:SessionIndex");
        String serviceTicket = stNode.getText();
        logger.info("登出的ST为{}", serviceTicket);
        //模拟删除服务器用户session的操作
        LOGIN_SESSION.remove(serviceTicket);
        //返回成功response
        response.setStatus(HttpStatus.OK.value());
    }
}

