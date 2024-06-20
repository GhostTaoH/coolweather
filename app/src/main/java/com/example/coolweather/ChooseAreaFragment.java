package com.example.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;
import org.litepal.exceptions.DataSupportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    /*
        这是一个用于遍历省市县数据的碎片
        由于遍历全国省市县的功能我们在后面还会复用，
        因此就不写在活动里面了，而是写在碎片里面，这样需要复用的时候直接在布局里面引用碎片就可以了
    */
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private AlertDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    /*
        为ListView创建一个适配器对象
        适配器是数据与视图的桥梁，数据在适配器中进行处理，然后以指定的方式展示在页面中
    */
    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /*
        省列表
    */
    private List<Province> provinceList;

    /*
        市列表
    */
    private List<City> cityList;

    /*
        县列表
    */
    private List<County> countyList;

    /*
        选中的省份
    */
    private Province selectedProvince;

    /*
       选中的城市
   */
    private City selectedCity;

    /*
       当前选中的级别
   */
    private int currentLevel;

    @Nullable
    @Override
    /*
        回调方法，在视图创建布局(加载布局)时调用
    */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        /*
            使用 LayoutInflater 中的 inflate()方法将一个 xml布局文件转换为 View对象，并是否添加到父布局中
            container: 这是一个 ViewGroup 对象，表示我们要将解析的布局添加到的父容器
            attachToRoot: 这是一个布尔值，表示是否将解析的布局立即添加到 container 中，false表示仅获取 View对象
        */
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        /*
            为 ListView 初始化和设置适配器
            android.R.layout.simple_list_item_1: 这是 Android 系统提供的一个内置布局资源 ID，表示一个简单的列表项布局。这个布局文件包含一个 TextView，用来显示列表项的数据。
            dataList 是要显示的数据源，适配器会根据这个数据源中的内容来生成列表项
        */
        adapter=new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    /*
        由于 onActivityCreated()回调方法已被弃用，可使用 onViewCreated()进行替代
        在 onCreateView 方法之后调用，用于初始化与视图相关的逻辑
    */
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
            setOnItemClickListener()方法:当单击并保持此AdapterView中的某个项目时，注册要调用的回调
        */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                } else if (currentLevel==LEVEL_CITY) {
                    selectedCity=cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                } else if (currentLevel==LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /*
        查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
    */
    private void queryProvinces() {
        Log.d("ChooseAreaFragment", "Querying provinces from database");
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        /*
            使用 LitePal 库从本地数据库中加载所有的省份数据。
        */
        provinceList= LitePal.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for (Province province:provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            /*
                设置当前选择的项目。 如果在触摸模式下，该项目不会被选中，但它仍然会被适当定位。 如果指定的选择位置小于0，则选择位置0处的项目。
                将 ListView 的选择位置设置为第一个项目。具体来说，它会使 ListView 显示从第一个项目开始，确保第一个项目在可见区域内（滚动到顶部）
            */
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            Log.d("ChooseAreaFragment", "Querying provinces from server");
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /*
        查询全国所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
    */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        /*
            使用 LitePal 库从数据库中查询特定省份的所有城市,使用 where()方法指定查询条件
            用于查询 City 表中所有 provinceid 字段与 selectedProvince.getId() 匹配的记录，返回这些记录组成的 City 对象列表。
        */
        cityList=LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for (City city:cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /*
        查询全国所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
    */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
        if(!countyList.isEmpty()){
            dataList.clear();
            for (County county:countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /*
        根据传入的地址和类型从服务器上查询省市县数据
    */
    private void queryFromServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                /*
                    用于从 response 对象中获取响应内容并将其转换为字符串。
                    具体来说，它从一个 HTTP 响应对象中读取响应体，并将其内容作为字符串存储在 responseText 变量中
                    response.body() 返回一个 ResponseBody 对象，它包含了响应的主体内容
                    response.body().string() 将 ResponseBody 对象的内容读取为一个字符串；会消耗流
                */
                String responseText=response.body().string();
                Log.d("ChooseAreaFragment", "Response from server: " + responseText);
                boolean result=false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                /*
                    runOnUiThread()方法返回主线程中处理逻辑
                */
                Log.e("ChooseAreaFragment", "Error querying from server", e);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /*
        显示进度对话框
    */
    private void showProgressDialog() {
//        检查确保进度对话框只被创建一次，如果它已经存在就不会重复创建。
        if(progressDialog==null){
//            创建一个 AlertDialog.Builder 实例。
            AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
            LayoutInflater inflater=getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_progress, null);
            builder.setView(view);
//            设置对话框在触摸外部区域时不会被取消。
            builder.setCancelable(false);
//            使用 builder.create() 方法创建 AlertDialog 实例，并将其赋值给 progressDialog
            progressDialog=builder.create();
        }
        progressDialog.show();
    }

    /*
        关闭进度对话框
    */
    private void closeProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


}
