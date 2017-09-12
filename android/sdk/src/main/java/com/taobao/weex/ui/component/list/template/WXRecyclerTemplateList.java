/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.weex.ui.component.list.template;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.Component;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.common.Constants;
import com.taobao.weex.common.ICheckBindingScroller;
import com.taobao.weex.common.OnWXScrollListener;
import com.taobao.weex.dom.ImmutableDomObject;
import com.taobao.weex.dom.WXCellDomObject;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.dom.WXEvent;
import com.taobao.weex.dom.WXRecyclerDomObject;
import com.taobao.weex.dom.binding.WXStatement;
import com.taobao.weex.dom.flex.Spacing;
import com.taobao.weex.el.parse.ArrayStack;
import com.taobao.weex.ui.component.AppearanceHelper;
import com.taobao.weex.ui.component.Scrollable;
import com.taobao.weex.ui.component.WXBaseRefresh;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXLoading;
import com.taobao.weex.ui.component.WXRefresh;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.component.binding.Layouts;
import com.taobao.weex.ui.component.binding.Statements;
import com.taobao.weex.ui.component.list.WXCell;
import com.taobao.weex.ui.view.listview.ExtendedLinearLayoutManager;
import com.taobao.weex.ui.view.listview.WXRecyclerView;
import com.taobao.weex.ui.view.listview.adapter.IOnLoadMoreListener;
import com.taobao.weex.ui.view.listview.adapter.IRecyclerAdapterListener;
import com.taobao.weex.ui.view.listview.adapter.RecyclerViewBaseAdapter;
import com.taobao.weex.ui.view.listview.adapter.TransformItemDecoration;
import com.taobao.weex.ui.view.listview.adapter.WXRecyclerViewOnScrollListener;
import com.taobao.weex.ui.view.refresh.wrapper.BounceRecyclerView;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * todo-list
 * 1、appear事件
 * 2、sticky事件
 * 3、多列测试
 * 5、for loop
 * weex template list supported
 * https://github.com/Hanks10100/weex-native-directive
 * demo address
 * http://dotwe.org/vue/ee9b5bbbe11629f565372595951670ec
 *
 * http://dotwe.org/vue/87b258b63599136999905a3e65c6c444
 *
 * http://dotwe.org/vue/94839a9b4e87c7c5464585f6a6c109ce
 *
 * http://dotwe.org/vue/0e549fb446458ecdc21e6115ea3a3c7b
 *
 * Created by jianbai.gbj on 2017/8/17.
 */
@Component(lazyload = false)
public class WXRecyclerTemplateList extends WXVContainer<BounceRecyclerView> implements
        IRecyclerAdapterListener<TemplateViewHolder>, IOnLoadMoreListener, Scrollable {
    public static final String TRANSFORM = "transform";
    private static final Pattern transformPattern = Pattern.compile("([a-z]+)\\(([0-9\\.]+),?([0-9\\.]+)?\\)");
    public static final String LOADMOREOFFSET = "loadmoreoffset";
    private static final String TAG = "WXRecyclerTemplateList";

    private WXRecyclerDomObject mDomObject;
    protected int mLayoutType = WXRecyclerView.TYPE_LINEAR_LAYOUT;
    protected int mColumnCount = 1;
    protected float mColumnGap = 0;
    protected float mColumnWidth = 0;
    private float mPaddingLeft;
    private float mPaddingRight;

    private WXRecyclerViewOnScrollListener mViewOnScrollListener = new WXRecyclerViewOnScrollListener(this);
    private int mListCellCount = 0;
    private boolean mForceLoadmoreNextTime = false;
    private RecyclerView.ItemAnimator mItemAnimator;

    /**
     * default orientation
     * */
    private  int orientation = Constants.Orientation.VERTICAL;

    /**
     * offset reported
     * */
    private boolean isScrollable = true;
    private int mOffsetAccuracy = 10;
    private Point mLastReport = new Point(-1, -1);

    private JSONArray listData;
    private String listDataKey = "listData";
    private String listDataItemKey ="item";
    private String listDataIndexKey = "index";
    private ArrayMap<String, Integer> mTemplateViewTypes;
    private Map<String, WXCell> mTemplates;
    private String  listDataTemplateKey = Constants.Name.SLOT_TEMPLATE_TYPE;
    private Runnable listUpdateRunnable;

    private int itemViewCacheSize = 4;

    /**
     * sticky helper
     * */
    private TemplateStickyHelper mStickyHelper;
    private List<String> mStickyTypes;


    /**
     * appear and disappear event managaer
     * */
    private ArrayMap<Integer, List<AppearanceHelper>> mAppearHelpers = new ArrayMap<>();

    /**
     * disappear event will be fire,
     * fist layer map key position,
     * send layer map key String ref
     * three layer map key view hash code, value is event arguments
     * */
    private ArrayMap<Integer, Map<String,Map<Integer, List<Object>>>> mDisAppearWatchList = new ArrayMap<>();

    public WXRecyclerTemplateList(WXSDKInstance instance, WXDomObject node, WXVContainer parent) {
        super(instance, node, parent);
        initRecyclerTemplateList(instance, node, parent);
    }

    private void initRecyclerTemplateList(WXSDKInstance instance, WXDomObject node, WXVContainer parent){
        if (node != null && node instanceof WXRecyclerDomObject) {
            mDomObject = (WXRecyclerDomObject) node;
            mDomObject.preCalculateCellWidth();
            mLayoutType = mDomObject.getLayoutType();
            updateRecyclerAttr();
        }
        mTemplateViewTypes = new ArrayMap<>();
        mTemplateViewTypes.put("", 0); //empty view, when template was not sended
        mTemplates = new HashMap<>();
        mStickyHelper = new TemplateStickyHelper(this);
        orientation = mDomObject.getOrientation();

    }

    @Override
    protected BounceRecyclerView initComponentHostView(@NonNull Context context) {
        final BounceRecyclerView bounceRecyclerView = new BounceRecyclerView(context,mLayoutType,mColumnCount,mColumnGap, getOrientation());
        String transforms = (String) getDomObject().getAttrs().get(TRANSFORM);
        if (transforms != null) {
            bounceRecyclerView.getInnerView().addItemDecoration(parseTransforms(transforms));
        }
        mItemAnimator = bounceRecyclerView.getInnerView().getItemAnimator();
        RecyclerViewBaseAdapter recyclerViewBaseAdapter = new RecyclerViewBaseAdapter<>(this);
        recyclerViewBaseAdapter.setHasStableIds(true);
        bounceRecyclerView.getInnerView().setItemViewCacheSize(itemViewCacheSize);
        bounceRecyclerView.setRecyclerViewBaseAdapter(recyclerViewBaseAdapter);
        bounceRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        bounceRecyclerView.getInnerView().clearOnScrollListeners();
        bounceRecyclerView.getInnerView().addOnScrollListener(mViewOnScrollListener);
        bounceRecyclerView.getInnerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                List<OnWXScrollListener> listeners = getInstance().getWXScrollListeners();
                if (listeners != null && listeners.size() > 0) {
                    for (OnWXScrollListener listener : listeners) {
                        if (listener != null) {
                            View topView = recyclerView.getChildAt(0);
                            if (topView != null) {
                                int y = topView.getTop();
                                listener.onScrollStateChanged(recyclerView, 0, y, newState);
                            }
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                List<OnWXScrollListener> listeners = getInstance().getWXScrollListeners();
                if (listeners != null && listeners.size() > 0) {
                    try {
                        for (OnWXScrollListener listener : listeners) {
                            if (listener != null) {
                                if (listener instanceof ICheckBindingScroller) {
                                    if (((ICheckBindingScroller) listener).isNeedScroller(getRef(), null)) {
                                        listener.onScrolled(recyclerView, dx, dy);
                                    }
                                } else {
                                    listener.onScrolled(recyclerView, dx, dy);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        bounceRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                BounceRecyclerView view;
                if ((view = getHostView()) == null)
                    return;
                mViewOnScrollListener.onScrolled(view.getInnerView(), 0, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        listUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                /**
                 * compute sticky position
                 * */
                if(mStickyHelper != null){
                    mStickyHelper.getStickyPositions().clear();
                    if(listData != null){
                        for(int i=0; i<listData.size(); i++){
                            WXCell cell = getSourceTemplate(i);
                            if(cell == null){
                                continue;
                            }
                            if(cell.isSticky()){
                                mStickyHelper.getStickyPositions().add(i);
                            }
                        }
                    }
                }
                if(getHostView() != null && getHostView().getRecyclerViewBaseAdapter() != null){
                    getHostView().getRecyclerViewBaseAdapter().notifyDataSetChanged();
                }
            }
        };
        return bounceRecyclerView;
    }


    @Override
    protected void onHostViewInitialized(BounceRecyclerView host) {
        super.onHostViewInitialized(host);
        WXRecyclerView recyclerView = host.getInnerView();
        if (recyclerView == null || recyclerView.getAdapter() == null) {
            WXLogUtils.e(TAG, "RecyclerView is not found or Adapter is not bound");
            return;
        }
    }

    /**
     * Measure the size of the recyclerView.
     *
     * @param width  the expected width
     * @param height the expected height
     * @return the result of measurement
     */
    @Override
    protected MeasureOutput measure(int width, int height) {
        int screenH = WXViewUtils.getScreenHeight(WXEnvironment.sApplication);
        int weexH = WXViewUtils.getWeexHeight(getInstanceId());
        int outHeight = height > (weexH >= screenH ? screenH : weexH) ? weexH - getAbsoluteY() : height;
        return super.measure((int)(width+mColumnGap), outHeight);
    }


    /**
     * These transform functions are supported:
     * - `scale(x,y)`: scale item, x and y should be a positive float number.
     * - `translate(x,y)`: translate item, `x` and `y` shoule be integer numbers.
     * - `opacity(n)`: change the transparency of item, `n` must in `[0,1.0]`.
     * - `rotate(n)`: rotate item, n is integer number.
     *
     * @param raw
     * @return
     */
    private RecyclerView.ItemDecoration parseTransforms(String raw) {
        if (raw == null) {
            return null;
        }
        float scaleX = 0f, scaleY = 0f;
        int translateX = 0, translateY = 0;
        float opacity = 0f;
        int rotate = 0;
        //public TransformItemDecoration(boolean isVertical,float alpha,int translateX,int translateY,int rotation,float scale)
        Matcher matcher = transformPattern.matcher(raw);
        while (matcher.find()) {
            String match = matcher.group();
            String name = matcher.group(1);
            try {
                switch (name) {
                    case "scale":
                        scaleX = Float.parseFloat(matcher.group(2));
                        scaleY = Float.parseFloat(matcher.group(3));
                        break;
                    case "translate":
                        translateX = Integer.parseInt(matcher.group(2));
                        translateY = Integer.parseInt(matcher.group(3));
                        break;
                    case "opacity":
                        opacity = Float.parseFloat(matcher.group(2));
                        break;
                    case "rotate":
                        rotate = Integer.parseInt(matcher.group(2));
                        break;
                    default:
                        WXLogUtils.e(TAG, "Invaild transform expression:" + match);
                        break;
                }
            } catch (NumberFormatException e) {
                WXLogUtils.e("", e);
                WXLogUtils.e(TAG, "Invaild transform expression:" + match);
            }
        }
        return new TransformItemDecoration(getOrientation() == Constants.Orientation.VERTICAL, opacity, translateX, translateY, rotate, scaleX, scaleY);
    }

    @Override
    public void bindStickStyle(WXComponent component) {
        WXComponent  template = findParentType(component, WXCell.class);
        if(template == null){
            return;
        }
        if(listData == null || mStickyHelper == null){
            return;
        }
        if(mStickyTypes == null){
            mStickyTypes = new ArrayList<>(4);
        }
        if(!mStickyTypes.contains(template.getRef())){
            mStickyTypes.add(template.getRef());
            notifyUpdateList();
        }
    }

    @Override
    public void unbindStickStyle(WXComponent component) {
        WXComponent  template = findParentType(component, WXCell.class);
        if(template == null
                || listData == null
                || mStickyHelper == null
                || mStickyTypes == null){
            return;
        }
        if(mStickyTypes.contains(template.getRef())){
            mStickyTypes.remove(template.getRef());
            notifyUpdateList();
        }
    }

    private @Nullable WXCell findCell(WXComponent component) {
        if(component instanceof  WXCell){
            return (WXCell) component;
        }
        WXComponent parent;
        if (component == null || (parent = component.getParent()) == null) {
            return null;
        }
        return findCell(parent);
    }

    private void setAppearanceWatch(WXComponent component, int event, boolean enable) {
        if(listData == null
                || mAppearHelpers == null
                || TextUtils.isEmpty(component.getRef())){
            return;
        }
        WXComponent cell = findCell(component);
        int type = getCellItemType(cell);
        if(type <= 0){
            return;
        }
        List<AppearanceHelper>  mAppearListeners = mAppearHelpers.get(type);
        if(mAppearListeners == null){
            mAppearListeners = new ArrayList<>();
            mAppearHelpers.put(type, mAppearListeners);
        }
        AppearanceHelper item = null;
        for(AppearanceHelper mAppearListener : mAppearListeners){
            if(component.getRef().equals(mAppearListener.getAwareChild().getRef())){
                item = mAppearListener;
                break;
            }
        }
        if (item != null) {
            item.setWatchEvent(event, enable);
        }else {
            item = new AppearanceHelper(component,  type);
            item.setWatchEvent(event, enable);
            mAppearListeners.add(item);
        }
    }

    @Override
    public void bindAppearEvent(WXComponent component) {
        setAppearanceWatch(component, AppearanceHelper.APPEAR, true);
    }

    @Override
    public void bindDisappearEvent(WXComponent component) {
        setAppearanceWatch(component, AppearanceHelper.DISAPPEAR, true);
    }

    @Override
    public void unbindAppearEvent(WXComponent component) {
        setAppearanceWatch(component, AppearanceHelper.APPEAR, false);
    }

    @Override
    public void unbindDisappearEvent(WXComponent component) {
        setAppearanceWatch(component, AppearanceHelper.DISAPPEAR, false);
    }

    @Override
    public void scrollTo(WXComponent component, Map<String, Object> options) {
        float offsetFloat = 0;
        boolean smooth = true;
        int position = -1;
        int typeIndex = -1;
        if (options != null) {
            String offsetStr = options.get(Constants.Name.OFFSET) == null ? "0" : options.get(Constants.Name.OFFSET).toString();
            smooth = WXUtils.getBoolean(options.get(Constants.Name.ANIMATED), true);
            if (offsetStr != null) {
                try {
                    offsetFloat = WXViewUtils.getRealPxByWidth(Float.parseFloat(offsetStr), getInstance().getInstanceViewPortWidth());
                }catch (Exception e ){
                    WXLogUtils.e("Float parseFloat error :"+e.getMessage());
                }
            }
            position = WXUtils.getNumberInt(options.get(Constants.Name.CELL_INDEX), position);
            typeIndex = WXUtils.getNumberInt(options.get(Constants.Name.TYPE_INDEX), position);
        }
        WXCell cell = findCell(component);
        if(typeIndex >= 0){
            if(listData != null && component.getRef() != null){
                int typePosition = 0;
                for(int i=0; i<listData.size(); i++){
                    WXCell template = getSourceTemplate(i);
                    if(template == null){
                        continue;
                    }
                    if(cell.getRef().equals(template.getRef())){
                        typePosition ++;
                    }
                    if(typePosition > typeIndex){
                        position = i;
                        break;
                    }
                }
                if(position < 0){
                    position = listData.size() - 1;
                }
            }
        }

        final int offset = (int) offsetFloat;
        BounceRecyclerView bounceRecyclerView = getHostView();
        if (bounceRecyclerView == null) {
            return;
        }
        if (position >= 0) {
            final int pos = position;
            final WXRecyclerView view = bounceRecyclerView.getInnerView();

            if (!smooth) {
                RecyclerView.LayoutManager layoutManager = view.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    //GridLayoutManager is also instance of LinearLayoutManager
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(pos, -offset);
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(pos, -offset);
                }
                //Any else?
            } else {
                if (offset != 0) {
                    view.setOnSmoothScrollEndListener(new ExtendedLinearLayoutManager.OnSmoothScrollEndListener() {
                        @Override
                        public void onStop() {
                            if (getOrientation() == Constants.Orientation.VERTICAL) {
                                view.smoothScrollBy(0, offset);
                            } else {
                                view.smoothScrollBy(offset, 0);
                            }
                        }
                    });
                }
                view.smoothScrollToPosition(pos);
            }
        }
    }

    @Override
    public int getScrollY() {
        BounceRecyclerView bounceRecyclerView = getHostView();
        return bounceRecyclerView == null ? 0 : bounceRecyclerView.getInnerView().getScrollY();
    }

    @Override
    public int getScrollX() {
        BounceRecyclerView bounceRecyclerView = getHostView();
        return bounceRecyclerView == null ? 0 : bounceRecyclerView.getInnerView().getScrollX();
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public boolean isScrollable() {
        return isScrollable;
    }



    @Override
    public void addChild(WXComponent child) {
        this.addChild(child, -1);
    }

    @Override
    protected int getChildrenLayoutTopOffset() {
        return 0;
    }

    @Override
    public void addChild(WXComponent child, int index) {
        super.addChild(child, index);
        if(child instanceof  WXBaseRefresh){
            return;
        }
        if(child instanceof WXCell){
            if(child.getDomObject() != null && child.getDomObject().getAttrs() != null){
                Object templateId = child.getDomObject().getAttrs().get(Constants.Name.SLOT_TEMPLATE_TYPE);
                String key = WXUtils.getString(templateId, null);
                if(key != null){
                    mTemplates.put(key, (WXCell) child);
                    if(mTemplateViewTypes.get(key) == null){
                        mTemplateViewTypes.put(key, mTemplateViewTypes.size());
                    }
                }
            }
            notifyUpdateList();
        }

        //FIXME adapter position not exist
        /**
        int adapterPosition = index == -1 ? mChildren.size() - 1 : index;
        BounceRecyclerView view = getHostView();
        if (view != null) {
            boolean isAddAnimation = isAddAnimation(child);
            if (isAddAnimation) {
                view.getInnerView().setItemAnimator(mItemAnimator);
            } else {
                view.getInnerView().setItemAnimator(null);
            }
            boolean isKeepScrollPosition = isKeepScrollPosition(child,index);
            if (isKeepScrollPosition) {
                int last=((LinearLayoutManager)view.getInnerView().getLayoutManager()).findLastVisibleItemPosition();
                view.getInnerView().getLayoutManager().scrollToPosition(last);
                view.getRecyclerViewBaseAdapter().notifyItemInserted(adapterPosition);
            } else {
                view.getRecyclerViewBaseAdapter().notifyItemChanged(adapterPosition);
            }
        }
        relocateAppearanceHelper();
         */
    }


    /**
     * Determine if the component needs to be fixed at the time of insertion
     * @param child Need to insert the component
     * @return fixed=true
     */
    private boolean isKeepScrollPosition(WXComponent child, int index) {
        ImmutableDomObject domObject = child.getDomObject();
        if (domObject != null) {
            Object attr = domObject.getAttrs().get(Constants.Name.KEEP_SCROLL_POSITION);
            if (WXUtils.getBoolean(attr, false) && index <= getChildCount() && index>-1) {
                return true;
            }
        }
        return false;
    }

    private void relocateAppearanceHelper() {
        /**
        Iterator<Map.Entry<String, AppearanceHelper>> iterator = mAppearHelpers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AppearanceHelper> item = iterator.next();
            AppearanceHelper value = item.getValue();
            WXComponent dChild = findCell(value.getAwareChild());
            int index = mChildren.indexOf(dChild);
            value.setCellPosition(index);
        }*/
    }

    /**
     * To determine whether an animation is needed
     * @param child
     * @return
     */
    private boolean isAddAnimation(WXComponent child) {
        ImmutableDomObject domObject = child.getDomObject();
        if (domObject != null) {
            Object attr = domObject.getAttrs().get(Constants.Name.INSERT_CELL_ANIMATION);
            if (Constants.Value.DEFAULT.equals(attr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * RecyclerView manage its children in a way that different from {@link WXVContainer}. Therefore,
     * {@link WXVContainer#addSubView(View, int)} is an empty implementation in {@link
     * com.taobao.weex.ui.view.listview.WXRecyclerView}
     */
    @Override
    protected void addSubView(View child, int index) {

    }

    /**
     * all child is template, none need create child except loading and refresh.
     * */
    @Override
    public void createChildViewAt(int index) {
        int indexToCreate = index;
        if (indexToCreate < 0) {
            indexToCreate = childCount() - 1;
            if (indexToCreate < 0) {
                return;
            }
        }
        final WXComponent child = getChild(indexToCreate);
        if (child instanceof WXBaseRefresh) {
            child.createView();
            setRefreshOrLoading(child);
        }
    }
    @Override
    public void remove(WXComponent child, boolean destroy) {
        removeFooterOrHeader(child);
        super.remove(child, destroy);

        /**
         * T view = getHostView();
         if (view == null) {
         return;
         }

         boolean isRemoveAnimation = isRemoveAnimation(child);
         if (isRemoveAnimation) {
         view.getInnerView().setItemAnimator(mItemAnimator);
         } else {
         view.getInnerView().setItemAnimator(null);
         }

         view.getRecyclerViewBaseAdapter().notifyItemRemoved(index);
         if (WXEnvironment.isApkDebugable()) {
         WXLogUtils.d(TAG, "removeChild child at " + index);
         }
         *
         * */
    }


    /**
     * To determine whether an animation is needed
     * @param child
     * @return
     */
    private boolean isRemoveAnimation(WXComponent child) {
        ImmutableDomObject domObject = child.getDomObject();
        if (domObject != null) {
            Object attr = domObject.getAttrs().get(Constants.Name.DELETE_CELL_ANIMATION);
            if (Constants.Value.DEFAULT.equals(attr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void computeVisiblePointInViewCoordinate(PointF pointF) {
        RecyclerView view = getHostView().getInnerView();
        pointF.set(view.computeHorizontalScrollOffset(), view.computeVerticalScrollOffset());
    }

    @Override
    protected boolean setProperty(String key, Object param) {
        switch (key) {
            case Constants.Name.LIST_DATA:{
                     setListData(param);
                }
                return true;
            case Constants.Name.LIST_DATA_TEMPLATE_KEY:
            case Constants.Name.SLOT_TEMPLATE_TYPE:
                listDataTemplateKey = WXUtils.getString(param, Constants.Name.SLOT_TEMPLATE_TYPE);
                return true;
            case LOADMOREOFFSET:
                return true;
            case Constants.Name.SCROLLABLE:
                boolean scrollable = WXUtils.getBoolean(param, true);
                setScrollable(scrollable);
                return true;
            case Constants.Name.SHOW_SCROLLBAR:
                Boolean result = WXUtils.getBoolean(param,null);
                if (result != null)
                    setShowScrollbar(result);
                return true;
            case "itemViewCacheSize":
                itemViewCacheSize = WXUtils.getNumberInt(param, itemViewCacheSize);
                return true;
            case Constants.Name.OFFSET_ACCURACY:
                int accuracy = WXUtils.getInteger(param, 10);
                setOffsetAccuracy(accuracy);
                return true;
        }
        return super.setProperty(key, param);
    }


    public void setListData(Object param){
        boolean update = listData != null;
        if(param instanceof  JSONArray){
            listData = (JSONArray) param;
        }else if (param instanceof  JSONObject){
            JSONObject data = null;
            Object repeat = ((JSONObject) param).getJSONObject(WXStatement.WX_FOR);
            if(repeat instanceof JSONObject){
                data = (JSONObject )repeat;
            }else{
                data = (JSONObject )param;
            }
            Object list = data.get(WXStatement.WX_FOR_LIST);
            if(list instanceof JSONArray){
                listData = (JSONArray) list;
                if(data.getString(WXStatement.WX_FOR_INDEX) != null) {
                    listDataIndexKey = data.getString(WXStatement.WX_FOR_INDEX);
                }
                if(data.getString(WXStatement.WX_FOR_ITEM) != null) {
                    listDataItemKey = data.getString(WXStatement.WX_FOR_ITEM);
                }
            }
        }
        if(update){
            notifyUpdateList();
        }
    }

    @WXComponentProp(name = Constants.Name.OFFSET_ACCURACY)
    public void setOffsetAccuracy(int accuracy) {
        float real = WXViewUtils.getRealPxByWidth(accuracy, getInstance().getInstanceViewPortWidth());
        this.mOffsetAccuracy = (int) real;
    }


    private void updateRecyclerAttr(){
        mColumnCount = mDomObject.getColumnCount();
        mColumnGap = mDomObject.getColumnGap();
        mColumnWidth = mDomObject.getColumnWidth();
        mPaddingLeft =mDomObject.getPadding().get(Spacing.LEFT);
        mPaddingRight =mDomObject.getPadding().get(Spacing.RIGHT);
    }

    @WXComponentProp(name = Constants.Name.COLUMN_WIDTH)
    public void setColumnWidth(int columnCount)  {
        if(mDomObject.getColumnWidth() != mColumnWidth){
            updateRecyclerAttr();
            WXRecyclerView wxRecyclerView = getHostView().getInnerView();
            wxRecyclerView.initView(getContext(), mLayoutType,mColumnCount,mColumnGap, getOrientation());
        }
    }

    @WXComponentProp(name = Constants.Name.SHOW_SCROLLBAR)
    public void setShowScrollbar(boolean show) {
        if(getHostView() == null || getHostView().getInnerView() == null){
            return;
        }
        if (getOrientation() == Constants.Orientation.VERTICAL) {
            getHostView().getInnerView().setVerticalScrollBarEnabled(show);
        } else {
            getHostView().getInnerView().setHorizontalScrollBarEnabled(show);
        }
    }

    @WXComponentProp(name = Constants.Name.COLUMN_COUNT)
    public void setColumnCount(int columnCount){
        if(mDomObject.getColumnCount() != mColumnCount){
            updateRecyclerAttr();
            WXRecyclerView wxRecyclerView = getHostView().getInnerView();
            wxRecyclerView.initView(getContext(), mLayoutType,mColumnCount,mColumnGap,getOrientation());
        }
    }

    @WXComponentProp(name = Constants.Name.COLUMN_GAP)
    public void setColumnGap(float columnGap) throws InterruptedException {
        if(mDomObject.getColumnGap() != mColumnGap) {
            updateRecyclerAttr();
            WXRecyclerView wxRecyclerView = getHostView().getInnerView();
            wxRecyclerView.initView(getContext(), mLayoutType, mColumnCount, mColumnGap, getOrientation());
        }
    }

    @WXComponentProp(name = Constants.Name.SCROLLABLE)
    public void setScrollable(boolean scrollable) {
        WXRecyclerView inner = getHostView().getInnerView();
        inner.setScrollable(scrollable);
    }

    @Override
    public void updateProperties(Map<String, Object> props) {
        super.updateProperties(props);
        if(props.containsKey(Constants.Name.PADDING)
                || props.containsKey(Constants.Name.PADDING_LEFT)
                || props.containsKey(Constants.Name.PADDING_RIGHT)){

            if(mPaddingLeft !=mDomObject.getPadding().get(Spacing.LEFT)
                    || mPaddingRight !=mDomObject.getPadding().get(Spacing.RIGHT)) {
                updateRecyclerAttr();
                WXRecyclerView wxRecyclerView = getHostView().getInnerView();
                wxRecyclerView.initView(getContext(), mLayoutType, mColumnCount, mColumnGap, getOrientation());
            }
        }
    }

    @JSMethod
    public void resetLoadmore() {
        mForceLoadmoreNextTime = true;
        mListCellCount = 0;
    }


    @Override
    public void addEvent(String type) {
        super.addEvent(type);
        if (Constants.Event.SCROLL.equals(type) && getHostView() != null && getHostView().getInnerView() != null) {
            WXRecyclerView innerView = getHostView().getInnerView();
            innerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private int offsetXCorrection, offsetYCorrection;
                private boolean mFirstEvent = true;

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (!layoutManager.canScrollVertically()) {
                        return;
                    }
                    int offsetX = recyclerView.computeHorizontalScrollOffset();
                    int offsetY = recyclerView.computeVerticalScrollOffset();

                    if (dx == 0 && dy == 0) {
                        offsetXCorrection = offsetX;
                        offsetYCorrection = offsetY;
                        offsetX = 0;
                        offsetY = 0;
                    } else {
                        offsetX = offsetX - offsetXCorrection;
                        offsetY = offsetY - offsetYCorrection;
                    }

                    if (mFirstEvent) {
                        //skip first event
                        mFirstEvent = false;
                        return;
                    }

                    if (shouldReport(offsetX, offsetY)) {
                        fireScrollEvent(recyclerView, offsetX, offsetY);
                    }
                }
            });
        }
    }

    private void fireScrollEvent(RecyclerView recyclerView, int offsetX, int offsetY) {
        offsetY = -calcContentOffset(recyclerView);
        int contentWidth = recyclerView.getMeasuredWidth() + recyclerView.computeHorizontalScrollRange();
        int contentHeight = calcContentSize();

        Map<String, Object> event = new HashMap<>(2);
        Map<String, Object> contentSize = new HashMap<>(2);
        Map<String, Object> contentOffset = new HashMap<>(2);

        contentSize.put(Constants.Name.WIDTH, WXViewUtils.getWebPxByWidth(contentWidth, getInstance().getInstanceViewPortWidth()));
        contentSize.put(Constants.Name.HEIGHT, WXViewUtils.getWebPxByWidth(contentHeight, getInstance().getInstanceViewPortWidth()));

        contentOffset.put(Constants.Name.X, - WXViewUtils.getWebPxByWidth(offsetX, getInstance().getInstanceViewPortWidth()));
        contentOffset.put(Constants.Name.Y, - WXViewUtils.getWebPxByWidth(offsetY, getInstance().getInstanceViewPortWidth()));
        event.put(Constants.Name.CONTENT_SIZE, contentSize);
        event.put(Constants.Name.CONTENT_OFFSET, contentOffset);

        fireEvent(Constants.Event.SCROLL, event);
    }

    private boolean shouldReport(int offsetX, int offsetY) {
        if (mLastReport.x == -1 && mLastReport.y == -1) {
            mLastReport.x = offsetX;
            mLastReport.y = offsetY;
            return true;
        }

        int gapX = Math.abs(mLastReport.x - offsetX);
        int gapY = Math.abs(mLastReport.y - offsetY);

        if (gapX >= mOffsetAccuracy || gapY >= mOffsetAccuracy) {
            mLastReport.x = offsetX;
            mLastReport.y = offsetY;
            return true;
        }

        return false;
    }


    /**
     * Setting refresh view and loading view
     *
     * @param child the refresh_view or loading_view
     */
    private boolean setRefreshOrLoading(final WXComponent child) {
        if (child instanceof WXRefresh && getHostView() != null) {
            getHostView().setOnRefreshListener((WXRefresh) child);
            getHostView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getHostView().setHeaderView(child);
                }
            }, 100);
            return true;
        }

        if (child instanceof WXLoading && getHostView() != null) {
            getHostView().setOnLoadingListener((WXLoading) child);
            getHostView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getHostView().setFooterView(child);
                }
            }, 100);
            return true;
        }
        return false;
    }


    private void removeFooterOrHeader(WXComponent child) {
        if (child instanceof WXLoading) {
            getHostView().removeFooterView(child);
        } else if (child instanceof WXRefresh) {
            getHostView().removeHeaderView(child);
        }
    }

    @Override
    public ViewGroup.LayoutParams getChildLayoutParams(WXComponent child, View hostView, int width, int height, int left, int right, int top, int bottom) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) hostView.getLayoutParams();
        if (child instanceof WXBaseRefresh && params == null) {
            params = new LinearLayout.LayoutParams(width, height);
        } else if (params == null) {
            params = new RecyclerView.LayoutParams(width, height);
        } else {
            params.width = width;
            params.height = height;
            params.setMargins(left, 0, right, 0);
        }
        return params;
    }


    @Override
    public void destroy() {
        if(getHostView() != null){
            getHostView().removeCallbacks(listUpdateRunnable);
        }
        if(listData != null){
            listData = null;
        }
        if(mStickyHelper != null){
            mStickyHelper = null;
        }
        if(mTemplateViewTypes != null){
            mTemplateViewTypes.clear();
        }
        if(mTemplates != null){
            mTemplates.clear();
        }
        if(mAppearHelpers != null){
            mAppearHelpers.clear();
        }
        if(mDisAppearWatchList != null){
            mDisAppearWatchList.clear();
        }
        super.destroy();
    }



    @Override
    public void onViewRecycled(TemplateViewHolder holder) {
    }

    @Override
    public void onBindViewHolder(final TemplateViewHolder templateViewHolder, int position) {
        if(templateViewHolder == null){
            return;
        }
        WXComponent component = templateViewHolder.getComponent();
        if(component == null){
            return;
        }
        if(getDomObject() instanceof  WXRecyclerDomObject){
            WXRecyclerDomObject domObject = (WXRecyclerDomObject) getDomObject();
            if(!domObject.hasPreCalculateCellWidth()){
                domObject.preCalculateCellWidth();
            }
            if(component.getDomObject() instanceof WXCellDomObject){
                WXCellDomObject cellDomObject = (WXCellDomObject) component.getDomObject();
                float w = ((WXRecyclerDomObject) domObject).getColumnWidth();
                cellDomObject.setLayoutWidth(w);
            }
        }
        long start = System.currentTimeMillis();
        templateViewHolder.setHolderPosition(position);
        Statements.doRender(component, getStackContextForPosition(position));
        if(WXEnvironment.isApkDebugable()){
            WXLogUtils.d(TAG, position + getTemplateKey(position) + " onBindViewHolder render used " + (System.currentTimeMillis() - start));
        }
        Layouts.doLayout(component, templateViewHolder.getLayoutContext());
        if(WXEnvironment.isApkDebugable()){
            WXLogUtils.d(TAG,  position + getTemplateKey(position) + " onBindViewHolder render used " + (System.currentTimeMillis() - start));
        }
    }

    @Override
    public TemplateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        long start = System.currentTimeMillis();
        String template = mTemplateViewTypes.keyAt(viewType);
        WXCell component = mTemplates.get(template);
        if(component != null){
            component = (WXCell) Statements.copyComponentTree(component);
        }
        if(component == null){
            FrameLayout view = new FrameLayout(getContext());
            view.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
            return new TemplateViewHolder(view, viewType);
        }
        component.lazy(false);
        component.createView();
        if(WXEnvironment.isApkDebugable()){
            WXLogUtils.d(TAG, template + " onCreateViewHolder view used " + (System.currentTimeMillis() - start));
        }
        component.applyLayoutAndEvent(component);
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG, template +  " onCreateViewHolder layout used " + (System.currentTimeMillis() - start));
        }
        component.bindData(component);
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG, template + " onCreateViewHolder bindData used " + (System.currentTimeMillis() - start));
        }
        TemplateViewHolder templateViewHolder = new TemplateViewHolder(component, viewType);
        return  templateViewHolder;
    }

    /**
     * @param position
     * when template not send, return an invalid id, use empty view holder.
     * when template has sended, use real template id to refresh view, use real view holder.
     * */
    @Override
    public int getItemViewType(int position) {
        JSONObject data = safeGetListData(position);
        String template = data.getString(listDataTemplateKey);
        if(TextUtils.isEmpty(template)){
            template = "";
        }
        int type =  mTemplateViewTypes.indexOfKey(template);
        if(type < 0){
            type = 0;
        }
        return type;
    }


    /**
     * return code context for render component
     * */
    private ArrayStack getStackContextForPosition(int position){
        ArrayStack stack = new ArrayStack();
        Map map = new HashMap();
        if(listData != null){
            stack.push(listData);
            stack.push(map);
            Object item = listData.get(position);
            stack.push(item);
            map.put(listDataIndexKey, position);
            map.put(listDataItemKey, item);
            map.put(listDataKey, listData);
        }
        return stack;
    }

    /**
     * return tepmlate key for position
     * */
    private String getTemplateKey(int position){
        JSONObject data =  safeGetListData(position);
        String template = data.getString(listDataTemplateKey);
        if(TextUtils.isEmpty(template)){
            template = "";
        }
        return  template;
    }

    /**
     * get source template
     * */
    private WXCell getSourceTemplate(int position){
        String template = getTemplateKey(position);
        return mTemplates.get(template);
    }

    /**
     * get template key from cell; 0  for default type
     * */
    private int getCellItemType(WXComponent cell){
        if(cell == null){
            return  -1;
        }
        if(cell.getDomObject() != null && cell.getDomObject().getAttrs() != null){
            Object templateId = cell.getDomObject().getAttrs().get(Constants.Name.SLOT_TEMPLATE_TYPE);
            String template = WXUtils.getString(templateId, null);
            if(template == null){
                return  0;
            }
            int type =  mTemplateViewTypes.indexOfKey(template);
            if(type < 0){
                return -1;
            }
            return  type;
        }
        return  0;
    }

    @Override
    public int getItemCount() {
        if(listData == null){
            return  0;
        }
        if(mTemplateViewTypes == null || mTemplateViewTypes.size() <= 1){
            return 0;
        }
        return listData.size();
    }

    @Override
    public boolean onFailedToRecycleView(TemplateViewHolder holder) {
        return false;
    }


    /**
     * @param position
     * when template not send by javascript, return an invalid id, force  use empty view holder.
     * when template has sended by javascript, use real template id to refresh view, use real view holder.
     * */
    @Override
    public long getItemId(int position) {
        if(getItemViewType(position) <= 0){
            return RecyclerView.NO_ID;
        }
        JSONObject data = safeGetListData(position);
        if(data.containsKey(Constants.Name.LIST_DATA_ITEM_ID)) {
            String itemKey = data.getString(Constants.Name.LIST_DATA_ITEM_ID);
            if(TextUtils.isEmpty(itemKey)){
                return  position;
            }
            return  itemKey.hashCode();
        }
        return position;
    }

    @Override
    public void onBeforeScroll(int dx, int dy) {
        if(mStickyHelper != null){
            mStickyHelper.onBeforeScroll(dx, dy);
        }
    }

    @Override
    public void onLoadMore(int offScreenY) {
        try {
            String offset = getDomObject().getAttrs().getLoadMoreOffset();

            if (TextUtils.isEmpty(offset)) {
                offset = "0";
            }
            float offsetParsed = WXViewUtils.getRealPxByWidth(Integer.parseInt(offset),getInstance().getInstanceViewPortWidth());

            if (offScreenY <= offsetParsed && listData != null) {
                if (mListCellCount != listData.size()
                        || mForceLoadmoreNextTime) {
                    fireEvent(Constants.Event.LOADMORE);
                    mListCellCount = listData.size();
                    mForceLoadmoreNextTime = false;
                }
            }
        } catch (Exception e) {
            WXLogUtils.d(TAG + "onLoadMore :", e);
        }
    }

    /**
     *
     * first fire appear event.
     * */
    @Override
    public void notifyAppearStateChange(int firstVisible, int lastVisible, int directionX, int directionY) {
        if(mAppearHelpers == null
                || mAppearHelpers.size() <= 0){
            return;
        }
        String direction = directionY > 0 ? Constants.Value.DIRECTION_UP :
                directionY < 0 ? Constants.Value.DIRECTION_DOWN : null;
        if (getOrientation() == Constants.Orientation.HORIZONTAL && directionX != 0) {
            direction = directionX > 0 ? Constants.Value.DIRECTION_LEFT : Constants.Value.DIRECTION_RIGHT;
        }
        RecyclerView recyclerView = getHostView().getInnerView();
        for(int position=firstVisible; position<=lastVisible; position++){
            int type = getItemViewType(position);
            List<AppearanceHelper> helpers = mAppearHelpers.get(type);
            if(helpers == null){
                continue;
            }
            for(AppearanceHelper helper : helpers){
                if(!helper.isWatch()){
                    continue;
                }
                TemplateViewHolder itemHolder = (TemplateViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                if(itemHolder == null){
                    break;
                }
                List<WXComponent> childListeners = findChildListByRef(itemHolder.getComponent(), helper.getAwareChild().getRef());
                if(childListeners == null || childListeners.size() == 0){
                    break;
                }

                Map<String, Map<Integer, List<Object>>> disAppearList = mDisAppearWatchList.get(position);
                if(disAppearList == null){
                    disAppearList = new ArrayMap<>();
                    mDisAppearWatchList.put(position, disAppearList);
                }

                Map<Integer, List<Object>> componentDisAppearList = disAppearList.get(helper.getAwareChild().getRef());
                if(componentDisAppearList == null){
                    componentDisAppearList = new ArrayMap<>();
                    disAppearList.put(helper.getAwareChild().getRef(), componentDisAppearList);
                }

                for(int m=0; m<childListeners.size(); m++){
                    WXComponent childLisener = childListeners.get(m);
                    if(childLisener.getHostView() == null){
                        continue;
                    }
                    boolean appear = helper.isViewVisible(childLisener.getHostView());
                    int key = childLisener.getHostView().hashCode();
                    if(appear){
                        if(!componentDisAppearList.containsKey(key)){
                            childLisener.notifyAppearStateChange(Constants.Event.APPEAR, direction);
                            List<Object> eventArgs = null;
                            if(childLisener.getDomObject().getEvents() != null
                                    && childLisener.getDomObject().getEvents().getEventBindingArgsValues() != null
                                    && childLisener.getDomObject().getEvents().getEventBindingArgsValues().get(Constants.Event.DISAPPEAR) != null){
                                eventArgs = childLisener.getDomObject().getEvents().getEventBindingArgsValues().get(Constants.Event.DISAPPEAR);
                            }
                            componentDisAppearList.put(key, eventArgs);
                        }
                    }else{
                        if(componentDisAppearList.containsKey(key)){
                            childLisener.notifyAppearStateChange(Constants.Event.DISAPPEAR, direction);
                            componentDisAppearList.remove(key);
                        }
                    }
                }
            }
        }

        //handle disappear event, out of position
        int count = getItemCount();
        for (int position=0; position<count; position++){
            if(position >= firstVisible && position <= lastVisible){
                position = lastVisible + 1;
                continue;
            }
            Map<String, Map<Integer, List<Object>>> map = mDisAppearWatchList.get(position);
            if(map == null){
                continue;
            }
            WXCell template = mTemplates.get(getTemplateKey(position));
            if(template == null){
                return;
            }
            Set<Map.Entry<String, Map<Integer, List<Object>>>> cellWatcherEntries = map.entrySet();
            for(Map.Entry<String,Map<Integer, List<Object>>> cellWatcherEntry : cellWatcherEntries){
                String ref = cellWatcherEntry.getKey();
                WXComponent component = findChildByRef(template, ref);
                if(component == null){
                    continue;
                }
                Map<Integer, List<Object>> eventWatchers = cellWatcherEntry.getValue();
                if(eventWatchers == null || eventWatchers.size() == 0){
                    continue;
                }
                WXEvent events = component.getDomObject().getEvents();
                Set<Map.Entry<Integer, List<Object>>> eventWatcherEntries = eventWatchers.entrySet();
                for(Map.Entry<Integer, List<Object>> eventWatcherEntry : eventWatcherEntries){
                    events.putEventBindingArgsValue(Constants.Event.DISAPPEAR, eventWatcherEntry.getValue());
                    component.notifyAppearStateChange(Constants.Event.DISAPPEAR, direction);
                }
                eventWatchers.clear();
            }
            mDisAppearWatchList.remove(position);
        }
    }


    private JSONObject safeGetListData(int position){
        try{
            return listData.getJSONObject(position);
        }catch (Exception e){return  JSONObject.parseObject("{}");}
    }

    private void  notifyUpdateList(){
        if(getHostView() != null){
            getHostView().removeCallbacks(listUpdateRunnable);
            getHostView().post(listUpdateRunnable);
        }
    }

    private int calcContentSize() {
        int totalHeight = 0;
        if(listData == null){
            return totalHeight;
        }
        for (int i = 0; i < listData.size(); i++) {
            WXCell child = getSourceTemplate(i);
            if (child != null) {
                totalHeight += child.getLayoutHeight();
            }
        }
        return totalHeight;
    }

    private int calcContentOffset(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            int offset = 0;
            for (int i=0;i<firstVisibleItemPosition;i++) {
                WXCell cell = getSourceTemplate(i);
                if(cell == null){
                    continue;
                }
                offset -= cell.getLayoutHeight();
            }

            if (layoutManager instanceof GridLayoutManager) {
                int spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
                offset = offset / spanCount;
            }
            View firstVisibleView = layoutManager.findViewByPosition(firstVisibleItemPosition);
            if(firstVisibleView != null) {
                offset += firstVisibleView.getTop();
            }
            return offset;
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int spanCount = ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
            int firstVisibleItemPosition = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null)[0];
            int offset = 0;
            for (int i=0;i<firstVisibleItemPosition;i++) {
                WXCell cell = getSourceTemplate(i);
                if(cell == null){
                    continue;
                }
                offset -= cell.getLayoutHeight();
            }
            offset = offset / spanCount;

            View firstVisibleView = layoutManager.findViewByPosition(firstVisibleItemPosition);
            if(firstVisibleView != null) {
                offset += firstVisibleView.getTop();
            }
            return offset;
        }
        return -1;
    }

    /**
     * find certain class type parent
     * */
    public  WXComponent findParentType(WXComponent component, Class type){
        if(component.getClass() == type){
            return component;
        }
        if(component.getParent() != null) {
            findTypeParent(component.getParent(), type);
        }
        return  null;
    }


    /**
     * find child by ref
     * */
    public WXComponent findChildByRef(WXComponent component, String ref){
        if(ref.equals(component.getRef())){
            return component;
        }
        if(component instanceof WXVContainer){
            WXVContainer container = (WXVContainer) component;
            for(int i=0; i<container.getChildCount(); i++){
                WXComponent child = findChildByRef(container.getChild(i), ref);
                if(child != null){
                    return  child;
                }
            }
        }
        return  null;
    }

    /**
     * find child list, has same ref
     * */
    public List<WXComponent> findChildListByRef(WXComponent component, String ref){
        WXComponent child = findChildByRef(component, ref);
        if(child == null){
            return  null;
        }
        List<WXComponent> componentList = new ArrayList<>();
        WXVContainer container = child.getParent();
        if(container != null && (!(container instanceof  WXRecyclerTemplateList))){
            for(int i=0; i<container.getChildCount(); i++){
                WXComponent   element = container.getChild(i);
                if(ref.equals(element.getRef())){
                    componentList.add(element);
                }
            }
        }else{
            componentList.add(child);
        }
        return  componentList;
    }
}