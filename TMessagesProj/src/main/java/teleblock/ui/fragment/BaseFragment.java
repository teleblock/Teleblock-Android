package teleblock.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;

import teleblock.event.BindEventBus;
import teleblock.ui.activity.BaseActivity;

public abstract class BaseFragment extends Fragment {

    public BaseActivity mActivity;
    protected View rootView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 获得全局的 Activity
        mActivity = (BaseActivity) requireActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //判断是否需要注册EventBus
        if (this.getClass().isAnnotationPresent(BindEventBus.class)) {
            EventBus.getDefault().register(this);
        }
        if (rootView == null) {
            rootView = getFrameLayout(inflater);
            onViewCreated();
        }
        //缓存的rootView需要判断是否已经被加过parent，如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //判断是否需要注销EventBus
        if (this.getClass().isAnnotationPresent(BindEventBus.class)) {
            EventBus.getDefault().removeAllStickyEvents();
            EventBus.getDefault().unregister(this);
        }
    }

    protected abstract View getFrameLayout(LayoutInflater inflater);

    protected abstract void onViewCreated();

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
