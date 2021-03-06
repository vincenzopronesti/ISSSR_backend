
package com.isssr.ticketing_system.interceptor.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.isssr.ticketing_system.exception.TokenExpiredException;
import com.isssr.ticketing_system.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Enumeration;


@Component
public class InterceptorConfig implements HandlerInterceptor {


   // @Value("${jwt.header}")
    private String tokenHeader="Authorization";


    //private static String authToken = null;
    private String requestedURI;
    private JwtTokenUtil jwtTokenUtil;






    /* Questo metodo viene eseguito che una richiesta HTTP sia processata da un REST Controller.
    * Esso preleva l'access token dall'header della richiesta e verifica se è scaduto. Se è così, ritorna false per far sì
    * che la richiesta non venga processata dall'handler del REST Controller. Altrimenti, effettua il refresh del token.*/
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        boolean res = true;


        /*******************************************************************************
         ** PRIMA DI OGNI RICHIESTA HTTP, NEL METODO PREHANDLE L'INTERCEPTOR           *
         * VA A VERIFICARE, TRAMITE JWTTOKENUTIL, SE IL TOKEN È SCADUTO. SE È SCADUTO, *
         * RITORNA FALSE E LA RICHIESTA NON VIENE ESEGUITA; VIENE SCRITTO NELL'HEADER  *
         * "expiration" PER INDICARE AL FRONT-END CHE È SCADUTO IL TOKEN.              *
         * ALTRIMENTI, VIENE EFFETTUATO IL REFRESH DEL TOKEN E VIENE SCRITTO IL VALORE *
         * DEL NUOVO TOKEN ALL'INTERNO DELLA RICHIESTA.                                *
         * IL TOKEN NON VIENE SALVATO MA VIENE OTTENUTO DA OGNI RICHIESTA HTTP         *
         *******************************************************************************/


        requestedURI = request.getRequestURI();
        if(!requestedURI.equals("/ticketingsystem/public/login/")){
            System.out.println("requestUri is: " + requestedURI);
            jwtTokenUtil = new JwtTokenUtil();
            String authToken = request.getHeader(tokenHeader);
            res = jwtTokenUtil.canTokenBeRefreshed(authToken);
            if(res == false) {
                response.getWriter().write("expiration");
            }
            else{
                authToken = jwtTokenUtil.refreshToken(authToken);
                response.setHeader(tokenHeader, authToken);
            }
            return res;

        }

        /*else{
            System.out.println("it's login");
        }*/



        return true;
    }
 

    /*public static void saveToken(String token){
        JWTToken = token;
    }*/


    @Override
    public void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {

        //System.out.println("postHandle!");
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        //System.out.println("afterCompletion() is invoked");
    }

}