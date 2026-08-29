package lmdb

import com.sun.jna.Pointer

internal object EnvCryptoHost {
    private var installed = false

    private val encCb = LmdbLibrary.KmdbEncHostCb { slot, src, srcLen, dst, dstLen, iv, ivLen, encdec ->
        val encryptor = EnvCryptoRegistry.encryptor(slot) ?: return@KmdbEncHostCb 22
        if (src == null || dst == null || iv == null) return@KmdbEncHostCb 22
        val srcBytes = src.getByteArray(0, srcLen)
        val ivBytes = iv.getByteArray(0, ivLen)
        val out = encryptor.transform(srcBytes, ivBytes, encdec != 0)
        if (out.size != dstLen) return@KmdbEncHostCb 22
        dst.write(0, out, 0, out.size)
        0
    }

    private val sumCb = LmdbLibrary.KmdbSumHostCb { slot, src, srcLen, dst, dstLen, key, keyLen ->
        val checksum = EnvCryptoRegistry.checksum(slot) ?: return@KmdbSumHostCb
        if (src == null || dst == null) return@KmdbSumHostCb
        val srcBytes = src.getByteArray(0, srcLen)
        val keyBytes = if (key != null && keyLen > 0) key.getByteArray(0, keyLen) else null
        val out = checksum.digest(srcBytes, keyBytes)
        if (out.size != dstLen) return@KmdbSumHostCb
        dst.write(0, out, 0, out.size)
    }

    @Synchronized
    fun install() {
        if (installed) return
        LmdbJna.kmdb_set_host_callbacks(encCb, sumCb)
        installed = true
    }
}
