package com.ssafy.connection.securityOauth.config.security.auth.company;

import com.ssafy.connection.securityOauth.config.security.auth.OAuth2UserInfo;
import com.ssafy.connection.securityOauth.domain.entity.user.Provider;

import java.util.Map;

public class Github extends OAuth2UserInfo{

    public Github(Map<String, Object> attributes) {
        super(attributes);
        //깃허브에서 받는 값 전부 출력
//        for(String key : attributes.keySet()){
//            Object value = attributes.get(key);
//            System.out.println(key+" : "+value);
//        }
    }

    @Override
    public String getId() {
        return (String) attributes.get("login");
//        return ((Integer) attributes.get("id")).toString();
    }

    @Override
    public String getName() {
        
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {

        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        
        return (String) attributes.get("avatar_url");
    }
    
    @Override
    public String getProvider(){
        return Provider.github.toString();
    }
}
