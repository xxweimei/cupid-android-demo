package com.example.kong.myapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v7.app.NotificationCompat;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;

/**
 * functional description
 * Created by Sandy
 * on 2016/11/9.
 */
class UdpChannelInitializer extends ChannelInitializer<DatagramChannel> {

    private NotificationManager mNotifyMgr;

    private NotificationCompat.Builder mBuilder;

    private static final String KEY = "master123";

    private OkHttpClient client = new OkHttpClient();

    private int id = 0;

    UdpChannelInitializer(NotificationManager mNotifyMgr, NotificationCompat.Builder mBuilder) {
        this.mNotifyMgr = mNotifyMgr;
        this.mBuilder = mBuilder;
    }

    @Override
    protected void initChannel(DatagramChannel ch) throws Exception {
        ch.pipeline().addLast("ping", new IdleStateHandler(0, 3, 0));
        ch.pipeline().addLast(new ChannelInboundHandler() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {

                ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(KEY, CharsetUtil.UTF_8)
                        , new InetSocketAddress("192.168.33.212", 55055)));

                Request request = new Request.Builder()
                        .url("http://192.168.33.212:9009/pull/" + KEY).build();

                Response response = client.newCall(request).execute();

                System.out.println("response" + response.body().string());
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                String body = ((DatagramPacket) msg).copy().content().toString(CharsetUtil.UTF_8);

                if (body.startsWith("##")) {
                    System.out.println(body);
                    mBuilder.setContentTitle("标题")
                            .setContentText(body.substring(2))
                            .setWhen(System.currentTimeMillis())
                            .setDefaults(Notification.DEFAULT_VIBRATE)
                            .setSmallIcon(R.mipmap.ic_launcher);
                    id++;
                    mNotifyMgr.notify(id, mBuilder.build());
                }
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof IdleStateEvent) {
                    IdleStateEvent event = (IdleStateEvent) evt;
                    if (event.state().equals(IdleState.READER_IDLE)) {
                        System.out.println("READER_IDLE");
                        // 超时关闭channel
                        ctx.close();
                    } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                        ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(KEY, CharsetUtil.UTF_8)
                                , new InetSocketAddress("192.168.33.212", 55055)));
                    } else if (event.state().equals(IdleState.ALL_IDLE)) {
                        System.out.println("ALL_IDLE");
                    }
                }
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            }

            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

            }
        });
    }
}
