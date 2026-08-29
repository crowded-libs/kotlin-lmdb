package lmdb

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal object EnvCryptoHost {
    private var installed = false

    private val encCb = staticCFunction {
            slot: Int,
            src: COpaquePointer?,
            srcLen: UInt,
            dst: COpaquePointer?,
            dstLen: UInt,
            iv: COpaquePointer?,
            ivLen: UInt,
            encdec: Int,
        ->
        val encryptor = EnvCryptoRegistry.encryptor(slot) ?: return@staticCFunction 22
        if (src == null || dst == null || iv == null) return@staticCFunction 22
        val srcBytes = src.readBytes(srcLen.toInt())
        val ivBytes = iv.readBytes(ivLen.toInt())
        val out = encryptor.transform(srcBytes, ivBytes, encdec != 0)
        if (out.size != dstLen.toInt()) return@staticCFunction 22
        out.usePinned { pinned ->
            memcpy(dst, pinned.addressOf(0), out.size.convert())
        }
        0
    }

    private val sumCb = staticCFunction {
            slot: Int,
            src: COpaquePointer?,
            srcLen: UInt,
            dst: COpaquePointer?,
            dstLen: UInt,
            key: COpaquePointer?,
            keyLen: UInt,
        ->
        val checksum = EnvCryptoRegistry.checksum(slot) ?: return@staticCFunction Unit
        if (src == null || dst == null) return@staticCFunction Unit
        val srcBytes = src.readBytes(srcLen.toInt())
        val keyBytes = if (key != null && keyLen > 0u) key.readBytes(keyLen.toInt()) else null
        val out = checksum.digest(srcBytes, keyBytes)
        if (out.size != dstLen.toInt()) return@staticCFunction Unit
        out.usePinned { pinned ->
            memcpy(dst, pinned.addressOf(0), out.size.convert())
        }
        Unit
    }

    fun install() {
        if (installed) return
        kmdb_set_host_callbacks(encCb, sumCb)
        installed = true
    }
}
