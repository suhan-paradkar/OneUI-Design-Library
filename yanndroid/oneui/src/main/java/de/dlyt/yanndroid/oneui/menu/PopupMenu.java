package de.dlyt.yanndroid.oneui.menu;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.MenuRes;

import java.util.ArrayList;

import de.dlyt.yanndroid.oneui.R;
import de.dlyt.yanndroid.oneui.sesl.utils.ReflectUtils;

public class PopupMenu {

    private Context context;
    private View anchor;
    private PopupMenuListener popupMenuListener;

    private Menu menu;
    private ArrayList<PopupMenuItemView> itemViews;
    private PopupWindow popupWindow;
    private PopupListView listView;
    private PopupMenuAdapter popupMenuAdapter;

    private LinearLayout layoutWithTitle;
    private CharSequence title;

    private int xoff = 0;
    private int yoff = 0;

    public PopupMenu(View anchor) {
        this.context = anchor.getContext();
        this.anchor = anchor;
    }

    public interface PopupMenuListener {
        boolean onMenuItemClick(MenuItem item);

        void onMenuItemUpdate(MenuItem menuItem);
    }

    public void setPopupMenuListener(PopupMenuListener listener) {
        popupMenuListener = listener;
    }

    public Menu getMenu() {
        return menu != null ? menu : (menu = new Menu());
    }

    public void inflate(@MenuRes int menuRes) {
        inflate(new Menu(menuRes, context));
    }

    public void inflate(@MenuRes int menuRes, CharSequence title) {
        inflate(new Menu(menuRes, context), title);
    }

    public void inflate(Menu menu) {
        inflate(menu, null);
    }

    public void inflate(Menu menu, CharSequence title) {
        this.menu = menu;
        this.title = title;

        if (popupWindow != null) {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
            popupWindow = null;
        }

        itemViews = new ArrayList<>();
        for (MenuItem menuItem : menu.menuItems)
            itemViews.add(new PopupMenuItemView(context, menuItem));

        popupMenuAdapter = new PopupMenuAdapter();
        listView = new PopupListView(context);
        listView.setAdapter(popupMenuAdapter);
        listView.setMaxHeight(context.getResources().getDimensionPixelSize(R.dimen.sesl_menu_popup_max_height));
        listView.setDivider(null);
        listView.setSelector(context.getResources().getDrawable(R.drawable.sesl_list_selector, context.getTheme()));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            MenuItem clickedMenuItem = menu.getItem(position);
            if (clickedMenuItem.isCheckable()) clickedMenuItem.toggleChecked();
            if (clickedMenuItem.hasSubMenu()) {
                PopupMenu subPopupMenu = new PopupMenu(anchor);
                subPopupMenu.inflate(clickedMenuItem.getSubMenu(), menu.getItem(position).getTitle());
                subPopupMenu.setPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return popupMenuListener.onMenuItemClick(item);
                    }

                    @Override
                    public void onMenuItemUpdate(MenuItem menuItem) {
                        popupMenuListener.onMenuItemUpdate(menuItem);
                    }
                });
                subPopupMenu.show(xoff, yoff);
                popupWindow.dismiss();
            } else if (popupMenuListener != null && popupMenuListener.onMenuItemClick(clickedMenuItem)) {
                popupWindow.dismiss();
            }
        });

        if (title != null) {
            layoutWithTitle = new LinearLayout(context);
            layoutWithTitle.setOrientation(LinearLayout.VERTICAL);
            layoutWithTitle.addView(createTitleView());
            layoutWithTitle.addView(listView);
            popupWindow = new PopupWindow(layoutWithTitle);
        } else {
            popupWindow = new PopupWindow(listView);
        }

        popupWindow.setWidth(getPopupMenuWidth());
        popupWindow.setHeight(getPopupMenuHeight());
        popupWindow.setAnimationStyle(R.style.MenuPopupAnimStyle);
        popupWindow.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.sesl_menu_popup_background, context.getTheme()));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 12, context.getResources().getDisplayMetrics()));
        popupWindow.setFocusable(true);
        if (popupWindow.isClippingEnabled()) popupWindow.setClippingEnabled(false);
        popupWindow.getContentView().setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() != KeyEvent.KEYCODE_MENU || keyEvent.getAction() != KeyEvent.ACTION_UP || !popupWindow.isShowing()) {
                return false;
            }
            popupWindow.dismiss();
            return true;
        });
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_OUTSIDE) {
                    return false;
                }
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private TextView createTitleView() {
        TextView titleView = new TextView(context);
        titleView.setPadding(context.getResources().getDimensionPixelSize(R.dimen.sesl_popup_menu_item_start_padding),
                context.getResources().getDimensionPixelSize(R.dimen.sesl_popup_menu_item_top_padding),
                context.getResources().getDimensionPixelSize(R.dimen.sesl_popup_menu_item_end_padding),
                context.getResources().getDimensionPixelSize(R.dimen.sesl_menu_popup_bottom_padding));
        titleView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        titleView.setTextColor(context.getResources().getColor(R.color.item_color));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setMaxLines(1);
        titleView.setTextSize(16);
        titleView.setText(title);
        return titleView;
    }

    public int getPopupMenuWidth() {
        int makeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = 0;

        for (int i = 0; i < popupMenuAdapter.getCount(); i++) {
            View view = itemViews.get(i);
            view.measure(makeMeasureSpec, makeMeasureSpec);
            int measuredWidth = view.getMeasuredWidth();
            if (measuredWidth > popupWidth) {
                popupWidth = measuredWidth;
            }
        }
        return popupWidth;
    }

    public int getPopupMenuHeight() {
        if (title == null) {
            listView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            return listView.getMeasuredHeight() + context.getResources().getDimensionPixelSize(R.dimen.sesl_popup_menu_item_bottom_padding) - 5;
        } else {
            layoutWithTitle.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            return layoutWithTitle.getMeasuredHeight() + context.getResources().getDimensionPixelSize(R.dimen.sesl_popup_menu_item_bottom_padding) - 5;
        }
    }

    private void updatePopupSize() {
        if (popupWindow.isShowing()) popupWindow.update(getPopupMenuWidth(), getPopupMenuHeight());
    }

    public void show() {
        show(0, 0);
    }

    public void show(int xoff, int yoff) {
        this.xoff = xoff;
        this.yoff = yoff;
        popupWindow.showAsDropDown(anchor, xoff, yoff);
        updatePopupSize();
        ((View) ReflectUtils.genericGetField(popupWindow, "mBackgroundView")).setClipToOutline(true);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    private class PopupMenuAdapter extends ArrayAdapter {

        public PopupMenuAdapter() {
            super(context, 0);
        }

        @Override
        public int getCount() {
            return menu.size();
        }

        @Override
        public View getView(int index, View view, ViewGroup parent) {
            MenuItem menuItem = menu.getItem(index);
            menuItem.setMenuItemListener(menuItem1 -> {
                itemViews.get(index).updateView();
                popupMenuListener.onMenuItemUpdate(menuItem);
                updatePopupSize();
            });
            return itemViews.get(index);
        }
    }

}
