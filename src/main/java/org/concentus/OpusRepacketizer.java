/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
   Ported to Java by Logan Stromberg

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of Internet Society, IETF or IETF Trust, nor the
   names of specific contributors, may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.concentus;

public class OpusRepacketizer {

    byte toc = 0;
    int nb_frames = 0;
    final byte[][] frames = new byte[48][];
    final short[] len = new short[48];
    int framesize = 0;

    /**
     * (Re)initializes a previously allocated repacketizer state. The state must
     * be at least the size returned by opus_repacketizer_get_size(). This can
     * be used for applications which use their own allocator instead of
     * malloc(). It must also be called to reset the queue of packets waiting to
     * be repacketized, which is necessary if the maximum packet duration of 120
     * ms is reached or if you wish to submit packets with a different Opus
     * configuration (coding mode, audio bandwidth, frame size, or channel
     * count). Failure to do so will prevent a new packet from being added with
     * opus_repacketizer_cat().
     *
     * @see opus_repacketizer_create
     * @see opus_repacketizer_get_size
     * @see opus_repacketizer_cat
     * @param rp <tt>OpusRepacketizer*</tt>: The repacketizer state to
     * (re)initialize.
     */
    public void Reset() {
        this.nb_frames = 0;
    }

    /**
     * Allocates memory and initializes the new repacketizer with
     * opus_repacketizer_init().
     */
    public OpusRepacketizer() {
        this.Reset();
    }

    int opus_repacketizer_cat_impl(byte[] data, int data_ptr, int len, int self_delimited) {
        BoxedValueByte dummy_toc = new BoxedValueByte((byte) 0);
        BoxedValueInt dummy_offset = new BoxedValueInt(0);
        int curr_nb_frames, ret;
        /* Set of check ToC */
        if (len < 1) {
            return OpusError.OPUS_INVALID_PACKET;
        }

        if (this.nb_frames == 0) {
            this.toc = data[data_ptr];
            this.framesize = OpusPacketInfo.getNumSamplesPerFrame(data, data_ptr, 8000);
        } else if ((this.toc & 0xFC) != (data[data_ptr] & 0xFC)) {
            /*fprintf(stderr, "toc mismatch: 0x%x vs 0x%x\n", rp.toc, data[0]);*/
            return OpusError.OPUS_INVALID_PACKET;
        }
        curr_nb_frames = OpusPacketInfo.getNumFrames(data, data_ptr, len);
        if (curr_nb_frames < 1) {
            return OpusError.OPUS_INVALID_PACKET;
        }

        /* Check the 120 ms maximum packet size */
        if ((curr_nb_frames + this.nb_frames) * this.framesize > 960) {
            return OpusError.OPUS_INVALID_PACKET;
        }

        ret = OpusPacketInfo.opus_packet_parse_impl(data, data_ptr, len, self_delimited, dummy_toc, this.frames, this.nb_frames, this.len, this.nb_frames, dummy_offset, dummy_offset);
        if (ret < 1) {
            return ret;
        }

        this.nb_frames += curr_nb_frames;
        return OpusError.OPUS_OK;
    }

    /**
     * opus_repacketizer_cat. Add a packet to the current repacketizer state.
     * This packet must match the configuration of any packets already submitted
     * for repacketization since the last call to opus_repacketizer_init(). This
     * means that it must have the same coding mode, audio bandwidth, frame
     * size, and channel count. This can be checked in advance by examining the
     * top 6 bits of the first byte of the packet, and ensuring they match the
     * top 6 bits of the first byte of any previously submitted packet. The
     * total duration of audio in the repacketizer state also must not exceed
     * 120 ms, the maximum duration of a single packet, after adding this
     * packet.
     *
     * The contents of the current repacketizer state can be extracted into new
     * packets using opus_repacketizer_out() or opus_repacketizer_out_range().
     *
     * In order to add a packet with a different configuration or to add more
     * audio beyond 120 ms, you must clear the repacketizer state by calling
     * opus_repacketizer_init(). If a packet is too large to add to the current
     * repacketizer state, no part of it is added, even if it contains multiple
     * frames, some of which might fit. If you wish to be able to add parts of
     * such packets, you should first use another repacketizer to split the
     * packet into pieces and add them individually.
     *
     * @see opus_repacketizer_out_range
     * @see opus_repacketizer_out
     * @see opus_repacketizer_init
     * @param data : The packet data. The application must ensure this pointer
     * remains valid until the next call to opus_repacketizer_init() or
     * opus_repacketizer_destroy().
     * @param len: The number of bytes in the packet data.
     * @returns An error code indicating whether or not the operation succeeded.
     * @retval #OPUS_OK The packet's contents have been added to the
     * repacketizer state.
     * @retval #OPUS_INVALID_PACKET The packet did not have a valid TOC
     * sequence, the packet's TOC sequence was not compatible with previously
     * submitted packets (because the coding mode, audio bandwidth, frame size,
     * or channel count did not match), or adding this packet would increase the
     * total amount of audio stored in the repacketizer state to more than 120
     * ms.
     */
    public int addPacket(byte[] data, int data_offset, int len) {
        return opus_repacketizer_cat_impl(data, data_offset, len, 0);
    }

    int opus_repacketizer_out_range_impl(int begin, int end,
            byte[] data, int data_ptr, int maxlen, int self_delimited, int pad) {
        int i, count;
        int tot_size;
        int ptr;

        if (begin < 0 || begin >= end || end > this.nb_frames) {
            /*fprintf(stderr, "%d %d %d\n", begin, end, rp.nb_frames);*/
            return OpusError.OPUS_BAD_ARG;
        }
        count = end - begin;

        if (self_delimited != 0) {
            tot_size = 1 + (this.len[count - 1] >= 252 ? 1 : 0);
        } else {
            tot_size = 0;
        }

        ptr = data_ptr;
        if (count == 1) {
            /* Code 0 */
            tot_size += this.len[0] + 1;
            if (tot_size > maxlen) {
                return OpusError.OPUS_BUFFER_TOO_SMALL;
            }
            data[ptr++] = (byte) (this.toc & 0xFC);
        } else if (count == 2) {
            if (this.len[1] == this.len[0]) {
                /* Code 1 */
                tot_size += 2 * this.len[0] + 1;
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL;
                }
                data[ptr++] = (byte) ((this.toc & 0xFC) | 0x1);
            } else {
                /* Code 2 */
                tot_size += this.len[0] + this.len[1] + 2 + (this.len[0] >= 252 ? 1 : 0);
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL;
                }
                data[ptr++] = (byte) ((this.toc & 0xFC) | 0x2);
                ptr += OpusPacketInfo.encode_size(this.len[0], data, ptr);
            }
        }
        if (count > 2 || (pad != 0 && tot_size < maxlen)) {
            /* Code 3 */
            int vbr;
            int pad_amount = 0;

            /* Restart the process for the padding case */
            ptr = data_ptr;
            if (self_delimited != 0) {
                tot_size = 1 + (this.len[count - 1] >= 252 ? 1 : 0);
            } else {
                tot_size = 0;
            }
            vbr = 0;
            for (i = 1; i < count; i++) {
                if (this.len[i] != this.len[0]) {
                    vbr = 1;
                    break;
                }
            }
            if (vbr != 0) {
                tot_size += 2;
                for (i = 0; i < count - 1; i++) {
                    tot_size += 1 + (this.len[i] >= 252 ? 1 : 0) + this.len[i];
                }
                tot_size += this.len[count - 1];

                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL;
                }
                data[ptr++] = (byte) ((this.toc & 0xFC) | 0x3);
                data[ptr++] = (byte) (count | 0x80);
            } else {
                tot_size += count * this.len[0] + 2;
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL;
                }
                data[ptr++] = (byte) ((this.toc & 0xFC) | 0x3);
                data[ptr++] = (byte) (count);
            }

            pad_amount = pad != 0 ? (maxlen - tot_size) : 0;

            if (pad_amount != 0) {
                int nb_255s;
                data[data_ptr + 1] = (byte) (data[data_ptr + 1] | 0x40);
                nb_255s = (pad_amount - 1) / 255;
                for (i = 0; i < nb_255s; i++) {
                    data[ptr++] = -1;
                }

                data[ptr++] = (byte) (pad_amount - 255 * nb_255s - 1);
                tot_size += pad_amount;
            }

            if (vbr != 0) {
                for (i = 0; i < count - 1; i++) {
                    ptr += (OpusPacketInfo.encode_size(this.len[i], data, ptr));
                }
            }
        }

        if (self_delimited != 0) {
            int sdlen = OpusPacketInfo.encode_size(this.len[count - 1], data, ptr);
            ptr += (sdlen);
        }

        /* Copy the actual data */
        for (i = begin; i < count + begin; i++) {

            if (data == this.frames[i]) {
                /* Using OPUS_MOVE() instead of OPUS_COPY() in case we're doing in-place
                   padding from opus_packet_pad or opus_packet_unpad(). */
                Arrays.MemMove(data, 0, ptr, this.len[i]);
            } else {
                System.arraycopy(this.frames[i], 0, data, ptr, this.len[i]);
            }
            ptr += this.len[i];
        }

        if (pad != 0) {
            /* Fill padding with zeros. */
            Arrays.MemSetWithOffset(data, (byte) 0, ptr, data_ptr + maxlen - ptr);
        }

        return tot_size;
    }

	/**
     * Pads a given Opus packet to a larger size (possibly changing the TOC
     * sequence).
     *
     * @param[in,out] data <tt>final unsigned char*</tt>: The buffer containing
     * the packet to pad.
     * @param len <tt>opus_int32</tt>: The size of the packet. This must be at
     * least 1.
     * @param new_len <tt>opus_int32</tt>: The desired size of the packet after
     * padding. This must be at least as large as len.
     * @returns an error code
     * @retval #OPUS_OK \a on success.
     * @retval #OPUS_BAD_ARG \a len was less than 1 or new_len was less than
     * len.
     * @retval #OPUS_INVALID_PACKET \a data did not contain a valid Opus packet.
     */
    public static int padPacket(byte[] data, int data_offset, int len, int new_len) {
        OpusRepacketizer rp = new OpusRepacketizer();
        int ret;
        if (len < 1) {
            return OpusError.OPUS_BAD_ARG;
        }
        if (len == new_len) {
            return OpusError.OPUS_OK;
        } else if (len > new_len) {
            return OpusError.OPUS_BAD_ARG;
        }
        rp.Reset();
        /* Moving payload to the end of the packet so we can do in-place padding */
        Arrays.MemMove(data, data_offset, data_offset + new_len - len, len);
        //data.MemMoveTo(data.Point(new_len - len), len);
        rp.addPacket(data, data_offset + new_len - len, len);
        ret = rp.opus_repacketizer_out_range_impl(0, rp.nb_frames, data, data_offset, new_len, 0, 1);
        if (ret > 0) {
            return OpusError.OPUS_OK;
        } else {
            return ret;
        }
    }

	// FIXME THIS METHOD FAILS IN TEST_OPUS_ENCODE
}
