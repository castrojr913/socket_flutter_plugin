package net.ongbut.socketflutterplugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

/** SocketFlutterPlugin */
public class SocketFlutterPlugin implements MethodCallHandler {
    /** Plugin registration. */

    private Socket mSocket;
    private MethodChannel channel;

    public SocketFlutterPlugin(MethodChannel channel) {
        this.channel = channel;
    }


    /** Plugin registration. */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "socket_flutter_plugin");
        channel.setMethodCallHandler(new SocketFlutterPlugin(channel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("socket")) {
            {
                try {
                    String url = call.argument("url");
                    mSocket = IO.socket(url);
                    Log.d("SocketIO ","Socket initialised");
                } catch (URISyntaxException e) {
                    Log.e("SocketIO ",e.toString());
                }
            }
            result.success("created");
        } else if (call.method.equals("connect")){
            mSocket.connect();
            Log.d("SocketIO  ","Connected");
            result.success("connected");
        } else if (call.method.equals("disconnect")){
            mSocket.disconnect();
            Log.d("SocketIO  ","Disconnected");
            result.success("disconnected");
        } else if (call.method.equals("emit")){
            String message = call.argument("message");
            String topic = call.argument("topic");
            JSONObject jb = null;
            try {
                jb = new JSONObject(message);
            } catch (JSONException e) {
                Log.e("SocketIO", e.toString());
            }
            if (jb != null){
                Log.d("SocketIO  ","Pushing " +  message + " __Topic__ " + topic);
                mSocket.emit(topic,jb);
                result.success("sent");
            } else {
                result.error("sent", "jb (JSONObject is null)", null);
            }

        } else if (call.method.equals("on")){
            String topic = call.argument("topic");
            Log.d("SocketIO  ","registering to "+ topic + " topic");
            mSocket.on(topic, onNewMessage);
            result.success("sent");
        }else if(call.method.equals("unsubscribe")){
            String topic = call.argument("topic");
            mSocket.off(topic);
        }else {
            result.notImplemented();
            Log.d("SocketIO ","Not Implemented");
        }
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Gson gson = new Gson();
            final String data = gson.toJson(args[0]);
            Log.d("SocketIO ", "Received " + data);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    channel.invokeMethod("received", new HashMap<String, String>(){{
                        put("message", data);
                    }});
                }
            });
        }
    };
}