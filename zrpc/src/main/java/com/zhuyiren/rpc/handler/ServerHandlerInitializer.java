package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.Server;
import com.zhuyiren.rpc.common.PacketDecoder;
import com.zhuyiren.rpc.common.PacketEncoder;
import com.zhuyiren.rpc.common.ServerIdleHandler;
import com.zhuyiren.rpc.engine.NormalEngine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/6/20.
 */
public class ServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOGGER= LoggerFactory.getLogger(ServerHandlerInitializer.class);

    private Server server;
    private StatisticsHandler statisticsHandler;
    private RequestHandlerAdapter handlerAdapter;

    private static final boolean useZip;

    static {
        useZip = Boolean.parseBoolean(System.getProperty("useZip", "false"));
        LOGGER.debug("Use zip:" + useZip);
    }


    public ServerHandlerInitializer(Server server) {
        this.server = server;
        statisticsHandler = new StatisticsHandler();
        initHandlers();
    }

    @Override

    protected void initChannel(SocketChannel ch) throws Exception {
        DefaultRequestDispatch requestDispatch = new DefaultRequestDispatch(handlerAdapter);
        for (Map.Entry<String, Object> entry : server.getServices().entrySet()) {
            requestDispatch.registerService(entry.getKey(), entry.getValue());
        }

        ch.pipeline().addLast(new IdleStateHandler(10,0,0));

        if (useZip) {
            ch.pipeline().addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(204800000, 0, 4, 0, 4));

        ch.pipeline().addLast(new PacketDecoder());


        if (useZip) {
            ch.pipeline().addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        }
        ch.pipeline().addLast(new LengthFieldPrepender(4));

        ch.pipeline()
                .addLast(new PacketEncoder())
                .addLast(statisticsHandler)
                .addLast(new ServerIdleHandler())
                .addLast(server.getBussinessExecutors(), requestDispatch);
    }


    public void initHandlers(){
        RequestHandlerAdapter compositeHandler=new RequestHandlerAdapterComposite();
        CommonRequestHandlerAdapter commonHandler = new CommonRequestHandlerAdapter();
        commonHandler.addEngine(new NormalEngine());
        commonHandler.addEngine(new ProtostuffEngine());
        compositeHandler.addHandlerAdapter(commonHandler);
        this.handlerAdapter=compositeHandler;
    }
}
