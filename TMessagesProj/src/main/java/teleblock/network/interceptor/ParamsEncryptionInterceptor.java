package teleblock.network.interceptor;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.EncryptUtils;
import com.hjq.http.config.IRequestInterceptor;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;
import com.hjq.http.request.HttpRequest;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;

import teleblock.config.AppConfig;
import teleblock.manager.LoginManager;
import teleblock.util.MMKVUtil;
import teleblock.util.ManifestUtil;
import teleblock.util.VersionUtil;

/**
 * Time:2022/6/20
 * Author:Perry
 * Description：请求参数加密拦截器
 */
public class ParamsEncryptionInterceptor implements IRequestInterceptor {

    @Override
    public void interceptArguments(@NonNull HttpRequest<?> httpRequest, @NonNull HttpParams params,
                                   @NonNull HttpHeaders headers) {
        if (httpRequest.getRequestHost().getHost().contains("https://api.opensea.io")) {
            headers.getHeaders().put("Accept", "application/json");
            headers.getHeaders().put("X-API-KEY", MMKVUtil.getSystemMsg().openseaapikey);
            return;
        } else if (!httpRequest.getRequestHost().getHost().contains(AppConfig.NetworkConfig.API_BASE_URL)) {
            return;
        }

        //公共参数+签名
        String wg_key = AppConfig.NetworkConfig.WG_KEY;
        String wg_name = AppConfig.NetworkConfig.WG_NAME;

        String wg_time = (System.currentTimeMillis() / 1000 + "");
        String md5Sign = EncryptUtils
                .encryptMD5ToString("wg_key=" + wg_key + "wg_name=" + wg_name + "wg_time=" + wg_time)
                .toLowerCase();
        params.put("wg_time", wg_time);
        params.put("wg_sign", md5Sign);
        params.put("platform", AppConfig.OS_TYPE + "");
        params.put("version", VersionUtil.getAppVersionName(ApplicationLoader.applicationContext));
        params.put("channel", ManifestUtil.getChannel(ApplicationLoader.applicationContext) + "");
        params.put("region", BuildConfig.REGION_AREA);
        params.put("package", AppUtils.getAppPackageName());

        //请求头
        headers.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");

        if (httpRequest.getRequestApi().getApi().contains("system/check")) {
            return;
        }

        if (!headers.getHeaders().containsKey("Authorization") && !TextUtils.isEmpty(LoginManager.getUserToken())) {
            headers.put("Authorization", "Bearer " + LoginManager.getUserToken());
        }
    }
}
