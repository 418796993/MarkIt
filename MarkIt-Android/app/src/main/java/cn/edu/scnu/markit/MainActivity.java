package cn.edu.scnu.markit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.FindListener;
import cn.edu.scnu.markit.adapter.LatestNoteAdapter;
import cn.edu.scnu.markit.adapter.RecordAdapter;
import cn.edu.scnu.markit.adapter.SortAdapter;
import cn.edu.scnu.markit.floatwindow.FloatWindowService;
import cn.edu.scnu.markit.javabean.CharacterParser;
import cn.edu.scnu.markit.javabean.Contact;
import cn.edu.scnu.markit.javabean.LatestNoteOfContacts;
import cn.edu.scnu.markit.javabean.Note;
import cn.edu.scnu.markit.javabean.PinyinComparator;
import cn.edu.scnu.markit.javabean.SortModel;
import cn.edu.scnu.markit.javabean.User;
import cn.edu.scnu.markit.test.TestContactActivity;
import cn.edu.scnu.markit.ui.ContactNotesActivity;
import cn.edu.scnu.markit.ui.view.SideBar;
import cn.edu.scnu.markit.util.CommonUtils;
import cn.edu.scnu.markit.util.DataManager;
import cn.edu.scnu.markit.util.MyDatabaseManager;
import cn.edu.scnu.markit.util.SortContacts;


public class MainActivity extends AppCompatActivity
{
    // test user by kasper 16.4.21
    //不用的时候直接把改变量有关的方法或调用全部删除干净 ctrl + F
    Button userTest;
    private ListView sortListView;
    private SideBar sideBar;
    /**
     * 显示字母的TextView
     */
    private TextView dialog;
    private SortAdapter sortAdapter;

    /**
     * 汉字转换成拼音的类
     */
    private CharacterParser characterParser;
    private List<SortModel> SourceDateList;

    /**
     * 根据拼音来排列ListView里面的数据类
     */
    private PinyinComparator pinyinComparator;
    private android.support.v7.widget.Toolbar main_toolbar;
    private android.support.v7.widget.Toolbar contact_toolbar;

    /**主界面listView**/
    private ListView main_list;
    private RecordAdapter recordAdapter;

    /**主界面中联系人最新消息适配器**/
    private LatestNoteAdapter latestNoteAdapter;

    private Context context = this;

    /**map 保存联系人的姓名和id**/
    private Map<String,String> contactNameWithId = new HashMap<>();

    /**保存联系人最新笔记**/
    private  List<LatestNoteOfContacts> noteOfContacts = new ArrayList<>();

    /**数据加载时，显示**/
    private ProgressDialog mProgressDialog = null;

    /**判断数据是否下载完成**/
   public static final int DOWNLOAD_DATA_SUCCESS = 1;

    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User myUser = BmobUser.getCurrentUser(this,User.class);
        MyDatabaseManager.userId = myUser.getObjectId();

        //  清除所有数据
        MyDatabaseManager.deleteAllContacts();
        MyDatabaseManager.deleteAllNotes();

        Log.i("userId",MyDatabaseManager.userId);

        initProgressDialog();

        initHandler();

       /* SharedPreferences sharedPreferences = getSharedPreferences("Login", 0);
        boolean firstDownLoad = sharedPreferences.getBoolean("firstDownLoad", true);
*/
        boolean isNerConnected = CommonUtils.isNetworkAvailable(this);
        if (!isNerConnected){
            Toast.makeText(MainActivity.this,R.string.net_unAvailable,Toast.LENGTH_SHORT).show();
        }else {
            downLoadData();
        }



        // test user by kasper 16.4.21
    }

    /**
     * 初始化ProgressDialog
     */
    private void initHandler(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (DOWNLOAD_DATA_SUCCESS == msg.what){

                    leftDraw(); //左侧滑框
                    initViews();//主界面

                    mProgressDialog.dismiss();

                }
            }
        };
    }

    /**
     * 初始化progressDialog
     */
    private void initProgressDialog(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("正在加载数据,请稍后");
        mProgressDialog.setCancelable(false);

    }

    /**
     * 初始化界面
     */
    private void initViews(){
        main_toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        main_list = (ListView) findViewById(R.id.listView2);

        noteOfContacts = getRecordData();
        latestNoteAdapter = new LatestNoteAdapter(this,noteOfContacts);

        main_list.setAdapter(latestNoteAdapter);
        main_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                LatestNoteOfContacts latestNoteOfContacts = (LatestNoteOfContacts)main_list.getItemAtPosition(position);
                String contactId = latestNoteOfContacts.getContactId();
                String contactName = latestNoteOfContacts.getContactName();

                Intent intent = new Intent(MainActivity.this, ContactNotesActivity.class);
                intent.putExtra("contactId",contactId);
                intent.putExtra("contactName",contactName);

                startActivity(intent);

                finish();   //跳转时，关闭
            }
        });

        //setSupportActionBar(main_toolbar);
        main_toolbar.setTitle("MarkIt");
        main_toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        main_toolbar.setNavigationIcon(R.drawable.contact);
        main_toolbar.inflateMenu(R.menu.menu_main_toolbar);
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);


        //创建返回键，并实现打开关/闭监听
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, main_toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        main_toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int menuItemId = item.getItemId();
                if (menuItemId == R.id.action_setting) {
                    Intent intent = new Intent(MainActivity.this, FloatWindowService.class);
                    startService(intent);

                    finish();   //  关闭主界面
                }else if (menuItemId == R.id.action_Add){
                    Intent intent = new Intent(MainActivity.this, TestContactActivity.class);
                    startActivity(intent);
                }
                return false;
            }
        });

    }
    private List<LatestNoteOfContacts> getRecordData() {

        List<LatestNoteOfContacts> noteOfContacts;
        noteOfContacts = MyDatabaseManager.queryLatestNoteForContacts();
        //noteOfContacts = DataSyncManager.queryLatestNotes(this);
        return noteOfContacts;

    }

    private void leftDraw() {
        contact_toolbar =(Toolbar) findViewById(R.id.contact_toolbar);
        //实例化汉字转拼音类
        characterParser = CharacterParser.getInstance();

        pinyinComparator = new PinyinComparator();

        sideBar = (SideBar) findViewById(R.id.sidebar);
        dialog = (TextView) findViewById(R.id.dialog);

        //setSupportActionBar(contact_toolbar);
        contact_toolbar.setNavigationIcon(R.drawable.addcontact);
        contact_toolbar.setTitle("联系人");
        contact_toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        contact_toolbar.inflateMenu(R.menu.menu_main);

        sideBar.setTextView(dialog);

        //设置右侧触摸监听
        sideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = sortAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    sortListView.setSelection(position);
                }

            }
        });

        sortListView = (ListView) findViewById(R.id.listView);
        sortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //这里要利用adapter.getItem(position)来获取当前position所对应的对象
                Toast.makeText(getApplication(), ((SortModel) sortAdapter.getItem(position)).getName(), Toast.LENGTH_SHORT).show();

            }
        });


        List<String> contactList;

        contactList = MyDatabaseManager.queryContacts();

        final int size = contactList.size();
        String[] contacts = (String[])contactList.toArray(new String[size]);
        SourceDateList = SortContacts.sortContactsByPinyin(contacts);

        // 根据a-z进行排序源数据
        Collections.sort(SourceDateList, pinyinComparator);
        sortAdapter = new SortAdapter(this, SourceDateList);
        sortListView.setAdapter(sortAdapter);



    }

    /**
     * 加载数据,先加载用户的联系人，存储在数据库中，再根据联系人，加载联系人对应的笔记
     */
    private void downLoadData(){

            mProgressDialog.show();

            User myUser = BmobUser.getCurrentUser(this, User.class);

            final String userId = myUser.getObjectId();

            Log.i("UserId------->",userId);

            BmobQuery<Contact> query = new BmobQuery<>();

            query.addWhereEqualTo("user", myUser);
            query.setLimit(100);
            query.addWhereEqualTo("isDelete", false);
            query.findObjects(this, new FindListener<Contact>() {
                @Override
                public void onSuccess(List<Contact> list) {

                    if (null != list){

                        DataManager dataManager = DataManager.getDataManager();
                        dataManager.setContactList(list);

                        for (int i=0; i<list.size(); i++){
                            Contact contact = list.get(i);
                            final String contactObjectId = contact.getObjectId();
                            final String contactName = contact.getContactName();

                            MyDatabaseManager.insertContact(contactObjectId,contactName);   //存储到数据库

                            BmobQuery<Note> queryNotes = new BmobQuery<Note>();

                            queryNotes.addWhereEqualTo("contact", list.get(i));
                            queryNotes.addWhereEqualTo("isDelete", false);

                            queryNotes.findObjects(MainActivity.this, new FindListener<Note>() {
                                @Override
                                public void onSuccess(List<Note> list) {

                                    if (list.size() != 0){

                                        for (int j=0; j<list.size(); j++){
                                            String noteObjectId = list.get(j).getObjectId();
                                            String text = list.get(j).getText();
                                            String date = list.get(j).getUpdatedAt();

                                            Log.i("text",text);
                                            MyDatabaseManager.insertNote(noteObjectId,text,contactObjectId,contactName,date);
                                        }


                                    }else {
                                        Log.i("ListNote--->","size is 0");
                                    }

                                    Message msg = Message.obtain();
                                    msg.what = DOWNLOAD_DATA_SUCCESS;
                                    mHandler.sendMessage(msg);

                                }

                                @Override
                                public void onError(int i, String s) {
                                    Log.i("queryNoteError------>",s);
                                }
                            });
                        }

                    }else {
                        Log.i("TAG----->","联系人列表为空");
                    }
                }

                @Override
                public void onError(int i, String s) {
                    Log.i("queryContactError--->",s);
                }
            });

        }



}
