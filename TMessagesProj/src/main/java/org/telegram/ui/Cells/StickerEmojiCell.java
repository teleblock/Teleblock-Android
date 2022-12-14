/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumLockIconView;

public class StickerEmojiCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private BackupImageView imageView;
    private PremiumLockIconView premiumIconView;
    private TLRPC.Document sticker;
    private SendMessagesHelper.ImportingSticker stickerPath;
    private Object parentObject;
    private String currentEmoji;
    private TextView emojiTextView;
    private float alpha = 1;
    private boolean changingAlpha;
    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private long time;
    private boolean recent;
    private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);
    private int currentAccount = UserConfig.selectedAccount;
    private boolean fromEmojiPanel;
    private boolean isPremiumSticker;
    private float premiumAlpha = 1f;
    private boolean showPremiumLock;

    int stickerColor;

    public StickerEmojiCell(Context context, boolean isEmojiPanel) {
        this(context, isEmojiPanel, 66);
    }

    public StickerEmojiCell(Context context, boolean isEmojiPanel, int size) {
        super(context);

        fromEmojiPanel = isEmojiPanel;

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(size, size, Gravity.CENTER));

        emojiTextView = new TextView(context);
        emojiTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
       // addView(emojiTextView, LayoutHelper.createFrame(28, 28, Gravity.BOTTOM | Gravity.RIGHT));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));

        premiumIconView = new PremiumLockIconView(context, PremiumLockIconView.TYPE_STICKERS_PREMIUM_LOCKED);
        premiumIconView.setImageReceiver(imageView.getImageReceiver());
        premiumIconView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        premiumIconView.setImageReceiver(imageView.getImageReceiver());
        addView(premiumIconView, LayoutHelper.createFrame(24, 24, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0 ,0, 0, 0));
        setFocusable(true);
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public SendMessagesHelper.ImportingSticker getStickerPath() {
        return stickerPath != null && stickerPath.validated ? stickerPath : null;
    }

    public String getEmoji() {
        return currentEmoji;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean value) {
        recent = value;
    }

    public void setSticker(TLRPC.Document document, Object parent, boolean showEmoji) {
        setSticker(document, null, parent, null, showEmoji);
    }

    public void setSticker(SendMessagesHelper.ImportingSticker path) {
        setSticker(null, path, null, path.emoji, path.emoji != null);
    }

    public MessageObject.SendAnimationData getSendAnimationData() {
        ImageReceiver imageReceiver = imageView.getImageReceiver();
        if (!imageReceiver.hasNotThumb()) {
            return null;
        }
        MessageObject.SendAnimationData data = new MessageObject.SendAnimationData();
        int[] position = new int[2];
        imageView.getLocationInWindow(position);
        data.x = imageReceiver.getCenterX() + position[0];
        data.y = imageReceiver.getCenterY() + position[1];
        data.width = imageReceiver.getImageWidth();
        data.height = imageReceiver.getImageHeight();
        return data;
    }

    public void setSticker(TLRPC.Document document, SendMessagesHelper.ImportingSticker path, Object parent, String emoji, boolean showEmoji) {
        currentEmoji = emoji;
        isPremiumSticker = MessageObject.isPremiumSticker(document);
        if (isPremiumSticker) {
            premiumIconView.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            premiumIconView.setWaitingImage();
        }
        if (path != null) {
            stickerPath = path;
            if (path.validated) {
                imageView.setImage(ImageLocation.getForPath(path.path), "80_80", null, null, DocumentObject.getSvgRectThumb(Theme.key_dialogBackgroundGray, 1.0f), null, path.animated ? "tgs" : null, 0, null);
            } else {
                imageView.setImage(null, null, null, null, DocumentObject.getSvgRectThumb(Theme.key_dialogBackgroundGray, 1.0f), null, path.animated ? "tgs" : null, 0, null);
            }
            if (emoji != null) {
                emojiTextView.setText(Emoji.replaceEmoji(emoji, emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16), false));
                emojiTextView.setVisibility(VISIBLE);
            } else {
                emojiTextView.setVisibility(INVISIBLE);
            }
        } else if (document != null) {
            sticker = document;
            parentObject = parent;
            boolean isVideoSticker = MessageObject.isVideoSticker(document);
            TLRPC.PhotoSize thumb = (isVideoSticker && MessageObject.canAutoplayAnimatedSticker(document)) ? null : FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, fromEmojiPanel ? Theme.key_emptyListPlaceholder : Theme.key_windowBackgroundGray, fromEmojiPanel ? 0.2f : 1.0f);
            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (svgThumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, svgThumb, parentObject);
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, null, parentObject);
                }
            } else {
                if (svgThumb != null) {
                    if (thumb != null) {
                        imageView.setImage(ImageLocation.getForDocument(thumb, document), "66_66", "webp", svgThumb, parentObject);
                    } else {
                        imageView.setImage(ImageLocation.getForDocument(document), "66_66", "webp", svgThumb, parentObject);
                    }
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(thumb, document), "66_66", "webp", null, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", "webp", null, parentObject);
                }
            }

            if (emoji != null) {
                emojiTextView.setText(Emoji.replaceEmoji(emoji, emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16), false));
                emojiTextView.setVisibility(VISIBLE);
            } else if (showEmoji) {
                boolean set = false;
                for (int a = 0; a < document.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                    if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                        if (attribute.alt != null && attribute.alt.length() > 0) {
                            emojiTextView.setText(Emoji.replaceEmoji(attribute.alt, emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16), false));
                            set = true;
                        }
                        break;
                    }
                }
                if (!set) {
                    emojiTextView.setText(Emoji.replaceEmoji(MediaDataController.getInstance(currentAccount).getEmojiForSticker(sticker.id), emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16), false));
                }
                emojiTextView.setVisibility(VISIBLE);
            } else {
                emojiTextView.setVisibility(INVISIBLE);
            }
        }
        updatePremiumStatus(false);
        imageView.getImageReceiver().setAlpha(alpha * premiumAlpha);
    }

    private void updatePremiumStatus(boolean animated) {
        if (isPremiumSticker) {
            showPremiumLock = true;
        } else {
            showPremiumLock = false;
        }
        FrameLayout.LayoutParams layoutParams = (LayoutParams) premiumIconView.getLayoutParams();
        if (!UserConfig.getInstance(currentAccount).isPremium()) {
            layoutParams.height = layoutParams.width = AndroidUtilities.dp(24);
            layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            layoutParams.bottomMargin = layoutParams.rightMargin = 0;
            premiumIconView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        } else {
            layoutParams.height = layoutParams.width = AndroidUtilities.dp(16);
            layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            layoutParams.bottomMargin = AndroidUtilities.dp(8);
            layoutParams.rightMargin = AndroidUtilities.dp(8);
            premiumIconView.setPadding(AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1));
        }
        premiumIconView.setLocked(!UserConfig.getInstance(currentAccount).isPremium());
        AndroidUtilities.updateViewVisibilityAnimated(premiumIconView, showPremiumLock, 0.9f, animated);
        invalidate();
    }

    public void disable() {
        changingAlpha = true;
        alpha = 0.5f;
        time = 0;
        imageView.getImageReceiver().setAlpha(alpha * premiumAlpha);
        imageView.invalidate();
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public boolean isDisabled() {
        return changingAlpha;
    }

    public boolean showingBitmap() {
        return imageView.getImageReceiver().getBitmap() != null;
    }

    public BackupImageView getImageView() {
        return imageView;
    }

    @Override
    public void invalidate() {
        emojiTextView.invalidate();
        super.invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
//        if (showPremiumLock && premiumAlpha > 0.5f) {
//            premiumAlpha -= 16 / 150f;
//            if (premiumAlpha < 0.5f) {
//                premiumAlpha = 0.5f;
//            }
//            invalidate();
//            imageView.invalidate();
//        } else if (!showPremiumLock && premiumAlpha < 1f) {
//            premiumAlpha += 16 / 150f;
//            if (premiumAlpha > 1f) {
//                premiumAlpha = 1f;
//            }
//            invalidate();
//            imageView.invalidate();
//        }
        imageView.getImageReceiver().setAlpha(alpha * premiumAlpha);

        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView && (changingAlpha || scaled && scale != 0.8f || !scaled && scale != 1.0f)) {
            long newTime = System.currentTimeMillis();
            long dt = (newTime - lastUpdateTime);
            lastUpdateTime = newTime;
            if (changingAlpha) {
                time += dt;
                if (time > 1050) {
                    time = 1050;
                }
                alpha = 0.5f + interpolator.getInterpolation(time / 150.0f) * 0.5f;
                if (alpha >= 1.0f) {
                    changingAlpha = false;
                    alpha = 1.0f;
                }
                imageView.getImageReceiver().setAlpha(alpha * premiumAlpha);
            } else if (scaled && scale != 0.8f) {
                scale -= dt / 400.0f;
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            } else {
                scale += dt / 400.0f;
                if (scale > 1.0f) {
                    scale = 1.0f;
                }
            }
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.invalidate();
            invalidate();
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        String descr = LocaleController.getString("AttachSticker", R.string.AttachSticker);
        if (sticker != null) {
            for (int a = 0; a < sticker.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = sticker.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                    if (attribute.alt != null && attribute.alt.length() > 0) {
                        emojiTextView.setText(Emoji.replaceEmoji(attribute.alt, emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16), false));
                        descr = attribute.alt + " " + descr;
                    }
                    break;
                }
            }
        }
        info.setContentDescription(descr);
        info.setEnabled(true);
    }

    public void showRequirePremiumAnimation() {
        if (premiumIconView != null) {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(200);
            }
            AndroidUtilities.shakeView(premiumIconView, 2, 0);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            updatePremiumStatus(true);
        }
    }
}
