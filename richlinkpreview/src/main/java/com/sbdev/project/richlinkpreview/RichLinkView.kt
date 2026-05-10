package com.sbdev.project.richlinkpreview

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.net.toUri
import com.sbdev.project.richlinkpreview.databinding.LinkLayoutBinding
import com.squareup.picasso.Picasso
import androidx.core.view.isNotEmpty

class RichLinkView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var meta: MetaData

    private var mainUrl: String? = null

    private var isDefaultClick = false

    private var richLinkListener: RichLinkListener? = null

    fun initView(){
        val binding = LinkLayoutBinding.inflate(LayoutInflater.from(context))

        if (meta.imageUrl.isBlank()) {
            binding.richLinkImage.visibility = GONE
        } else {
            binding.richLinkImage.visibility = VISIBLE
            Picasso.get()
                .load(meta.imageUrl)
                .into(binding.richLinkImage)
        }

        if (meta.title.isBlank()) {
            binding.richLinkTitle.visibility = GONE
        } else {
            binding.richLinkTitle.visibility = VISIBLE
            binding.richLinkTitle.text = meta.title
        }
        if (meta.url.isBlank()) {
            binding.richLinkUrl.visibility = GONE
        } else {
            binding.richLinkUrl.visibility = VISIBLE
            binding.richLinkUrl.text = meta.url
        }
        if (meta.description.isBlank()) {
            binding.richLinkDesp.visibility = GONE
        } else {
            binding.richLinkDesp.visibility = VISIBLE
            binding.richLinkDesp.text = meta.description
        }


        binding.richLinkCard.setOnClickListener { view ->
            if (isDefaultClick) {
                richLinkClicked()
            } else {
                if (richLinkListener != null) {
                    richLinkListener?.onClicked(view, meta)
                } else {
                    richLinkClicked()
                }
            }
        }
    }

    private fun richLinkClicked(){
        val intent = Intent(Intent.ACTION_VIEW, mainUrl?.toUri())
        context.startActivity(intent)
    }

    fun setDefaultClickListener(isDefault: Boolean){
        this.isDefaultClick = isDefault
    }

    fun setClickListener(richLinkListener: RichLinkListener){
        this.richLinkListener = richLinkListener
    }

    fun findLinearLayoutChild(): LinearLayout? {
        return if (isNotEmpty() && getChildAt(0) is LinearLayout) {
            getChildAt(0) as LinearLayout
        } else null
    }

    fun setLinkFromMeta(metaData: MetaData){
        meta = metaData
        initView()
    }

    fun setLink(url: String, viewListener: ViewListener) {
        mainUrl = url
        val richPreview = RichPreview(object: ResponseListener {
            override fun onData(metaData: MetaData) {
                meta = metaData
                if(meta.title.isNotBlank()) {
                    viewListener.onSuccess(true)
                }

                initView()
            }

            override fun onError(e: Exception) {
                viewListener.onError(e)
            }
        })

        richPreview.getPreview(url)
    }
}