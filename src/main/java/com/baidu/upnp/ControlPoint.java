/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.upnp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

/*
 * @description：
 * @author：
 * @createTime：2019-11-18 16:59
 */
class SetTargetActionInvocation extends ActionInvocation {


     SetTargetActionInvocation(Service service) {
        super(service.getAction("SetTarget"));
        try {

            // Throws InvalidValueException if the value is of wrong type
            setInput("NewTargetValue", true);
        } catch (InvalidValueException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}

class SetUrlActionInvocation extends ActionInvocation{
    public SetUrlActionInvocation(Service service) {
        super(service.getAction("SetAVTransportURI"));
        String arg1 = "InstanceID";
        String arg2 = "CurrentURI";
        String arg3 = "CurrentURIMetaData";

        ActionArgumentValue[] actionArgumentValues = {
                new ActionArgumentValue(getInputArgument(arg1), new UnsignedIntegerFourBytes(0)),
                new ActionArgumentValue(getInputArgument(arg2), "http://n.sinaimg.cn/news/1_img/upload/cf3881ab/311/w2048h1463/20191118/f31d-iipztfe6868704.jpg"),
                new ActionArgumentValue(getInputArgument(arg3), "")
        };
        setInput(actionArgumentValues);
    }
}
class PlayActionInvocation extends ActionInvocation{

    public PlayActionInvocation(Service service) {
        super(service.getAction("Play"));

        String arg1 = "InstanceID";
        String arg2 = "Speed";
        ActionArgumentValue[] actionArgumentValues = {
                new ActionArgumentValue(getInputArgument(arg1), new UnsignedIntegerFourBytes(0)),
                new ActionArgumentValue(getInputArgument(arg2), "1")
        };
        setInput(actionArgumentValues);
    }
}
public class ControlPoint implements Runnable {

    private void executeSetTargetAction(UpnpService upnpService, Service switchPowerService) {

        final ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(switchPowerService);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute(
                new ActionCallback(setTargetInvocation) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        assert invocation.getOutput().length == 0;
                        System.out.println("Successfully called set action!");
                        ActionArgumentValue newTargetValue = setTargetInvocation.getInput("NewTargetValue");
                        System.out.println("update value:"+newTargetValue);
                    }

                    @Override
                    public void failure(ActionInvocation invocation,
                                        UpnpResponse operation,
                                        String defaultMsg) {
                        System.err.println(defaultMsg);
                    }
                }
        );

    }

    private void executePlayAction(UpnpService upnpService, Service dlnaService)
            throws ExecutionException, InterruptedException {
        Future future = upnpService.getControlPoint().
                execute(new ActionCallback(new SetUrlActionInvocation(dlnaService)) {
            @Override
            public void success(ActionInvocation invocation) {

            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {

            }
        });
        future.get();
        upnpService.getControlPoint().execute(
                new ActionCallback(new PlayActionInvocation(dlnaService)) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        System.out.println("创维电视投屏成功！！");
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        System.out.println("创维电视投屏失败，" + defaultMsg);
                    }
                }
        );
    }

    private RegistryListener createRegistryListener(final UpnpService upnpService) {
        return new DefaultRegistryListener() {

            ServiceId serviceId = new UDAServiceId("SwitchPower");

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
//                Service switchPower;
//                if ((switchPower = device.findService(serviceId)) != null) {
//                    System.out.println("Service discovered: " + switchPower);
//                    executeSetTargetAction(upnpService,switchPower);
//                }
                System.out.println("Service discovered friendly name:" + device.getDetails().getFriendlyName());
                if(device.getDetails().getFriendlyName().equals("客厅电视-DLNA")){
//                if(device.getDetails().getFriendlyName().equals("客厅的小米电视")){
//                if(device.getDetails().getFriendlyName().equals("乐播投屏（小米）A4")){
                    System.out.println("发现创维电视了");
                    Service dlnaService=device.findService(device.getServices()[0].getServiceId());
                    try {
                        executePlayAction(upnpService,dlnaService);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }



            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                Service switchPower;
                if ((switchPower = device.findService(serviceId)) != null) {
                    System.out.println("Service disappeared: " + switchPower);
                }
            }

        };
    }

    @Override
    public void run() {
        UpnpService upnpService = new UpnpServiceImpl();
        upnpService.getRegistry().addListener(createRegistryListener(upnpService));
        upnpService.getControlPoint().search(new STAllHeader());
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new ControlPoint());
        thread.setDaemon(false);
        thread.start();
    }
}
