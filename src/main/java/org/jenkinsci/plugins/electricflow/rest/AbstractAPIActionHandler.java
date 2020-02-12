package org.jenkinsci.plugins.electricflow.rest;

import hudson.Extension;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

public class AbstractAPIActionHandler {
//public class AbstractAPIActionHandler<T> extends TransientActionFactory<T> implements Action {
    public static final String URL_BASE = "flowapi";

    // public T target;

    // @Override
//    public String getUrlName() {
//        System.out.println("geturlname");
//        return URL_BASE;
//    }
//
//    // @Override
//    public String getIconFileName() {
//        // No display
//        System.out.println("getIconFileName");
//        return null;
//    }
//
//    // @Override
//    public String getDisplayName() {
//        // No display
//        System.out.println("getDisplayName");
//        return null;
//    }
//
//    // @Override
//    public Class<CloudBeesFlowAPIHandler> type() {
//        System.out.println("GetType");
//        //return null;
//        return CloudBeesFlowAPIHandler.class;
//        // return (Class<CloudBeesFlowAPIHandler>) CloudBeesFlowAPIHandler.class;
//    }
//
//    @Nonnull
//    // @Override
//    public Collection<? extends Action> createFor(@Nonnull T t) {
//        System.out.println("GetCollection");
//        return null;
//    }
}