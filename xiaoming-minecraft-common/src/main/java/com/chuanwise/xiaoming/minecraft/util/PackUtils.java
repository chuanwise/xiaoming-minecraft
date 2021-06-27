package com.chuanwise.xiaoming.minecraft.util;

import com.chuanwise.xiaoming.minecraft.pack.PackType;
import com.chuanwise.xiaoming.minecraft.pack.content.IdContent;
import com.chuanwise.xiaoming.minecraft.pack.content.ResultContent;
import com.chuanwise.xiaoming.minecraft.socket.SocketController;

public class PackUtils {
    public static ResultContent sendResult(SocketController controller, IdContent reason, PackType type, boolean success, Object object) {
        return sendResult(controller, reason.getRequestId(), type, success, object);
    }

    public static ResultContent sendResult(SocketController controller, int requestId, PackType type, boolean success, Object object) {
        final ResultContent content = new ResultContent(success, object);
        sendResult(controller, requestId, type, content);
        return content;
    }

    public static IdContent sendResult(SocketController controller, int requestId, PackType type, IdContent content) {
        content.asResultOf(requestId);
        controller.sendLater(type, content);
        return content;
    }

    public static IdContent sendResult(SocketController controller, int requestId, PackType type, Object content) {
        return sendResult(controller, requestId, type, new IdContent(requestId, content));
    }

    public static ResultContent sendResult(SocketController controller, int requestId, PackType type, ResultContent content) {
        sendResult(controller, requestId, type, new IdContent(requestId, content));
        return content;
    }
}
