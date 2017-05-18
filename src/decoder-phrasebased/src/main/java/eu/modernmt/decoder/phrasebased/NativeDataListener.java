package eu.modernmt.decoder.phrasebased;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 08/02/17.
 */
class NativeDataListener implements DataListener {

    private final long nativeHandle;

    public NativeDataListener(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    @Override
    public void onDataReceived(TranslationUnit unit) throws Exception {
        String sourceSentence = XUtils.encodeSentence(unit.sourceSentence);
        String targetSentence = XUtils.encodeSentence(unit.targetSentence);
        int[] alignment = XUtils.encodeAlignment(unit.alignment);

        updateReceived(unit.channel, unit.channelPosition, unit.domain, sourceSentence, targetSentence, alignment);
    }

    private native void updateReceived(short channel, long channelPosition, int domain, String sourceSentence, String targetSentence, int[] alignment);

    @Override
    public void onDelete(Deletion deletion) throws Exception {
        deleteReceived(deletion.channel, deletion.channelPosition, deletion.domain);
    }

    private native void deleteReceived(short channel, long channelPosition, int domain);

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        long[] ids = getLatestUpdatesIdentifier();

        HashMap<Short, Long> map = new HashMap<>(ids.length);
        for (short i = 0; i < ids.length; i++) {
            if (ids[i] >= 0)
                map.put(i, ids[i]);
        }

        return map;
    }

    private native long[] getLatestUpdatesIdentifier();

}
