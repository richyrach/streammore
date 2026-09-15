package com.slop.since;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(245,245,247);
    private final int CARD = Color.WHITE;
    private final int TEXT = Color.rgb(20,20,22);
    private final int MUTED = Color.rgb(120,120,128);
    private final int BLUE = Color.rgb(0,122,255);
    private final int RED = Color.rgb(255,59,48);
    private LinearLayout list;
    private TextView sinceTab, scheduleTab, subtitle;
    private String mode = "since";
    private final ArrayList<Item> items = new ArrayList<>();
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("slop_since", MODE_PRIVATE);
        loadItems();
        if (items.isEmpty() && !prefs.getBoolean("seeded", false)) { seed(); prefs.edit().putBoolean("seeded", true).apply(); saveItems(); }
        buildUi(); render();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView eyebrow = text("tiny life tracker",13,MUTED,Typeface.BOLD); eyebrow.setLetterSpacing(.08f); root.addView(eyebrow);
        TextView title = text("Since",38,TEXT,Typeface.BOLD); title.setPadding(0,dp(3),0,0); root.addView(title);
        subtitle = text("how long has it been?",16,MUTED,Typeface.NORMAL); subtitle.setPadding(0,dp(1),0,dp(16)); root.addView(subtitle);
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(dp(4),dp(4),dp(4),dp(4)); tabs.setBackground(pill(Color.rgb(232,232,236),18));
        sinceTab = tab("Since"); scheduleTab = tab("Schedule"); tabs.addView(sinceTab,new LinearLayout.LayoutParams(0,dp(42),1)); tabs.addView(scheduleTab,new LinearLayout.LayoutParams(0,dp(42),1)); root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(50)));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(0,dp(12),0,dp(120)); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button add = new Button(this); add.setText("+  Add a thing"); add.setTextSize(16); add.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add.setTextColor(Color.WHITE); add.setAllCaps(false); add.setBackground(pill(TEXT,28)); add.setElevation(dp(8)); add.setOnClickListener(v->showItemDialog(null)); root.addView(add,new LinearLayout.LayoutParams(-1,dp(58)));
        sinceTab.setOnClickListener(v->switchMode("since")); scheduleTab.setOnClickListener(v->switchMode("schedule"));
        setContentView(root); updateTabs();
    }

    private TextView tab(String s){ TextView t=text(s,15,TEXT,Typeface.BOLD); t.setGravity(Gravity.CENTER); return t; }
    private void switchMode(String m){ mode=m; getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); updateTabs(); render(); }
    private void updateTabs(){ boolean s=mode.equals("since"); sinceTab.setBackground(s?pill(Color.WHITE,14):null); scheduleTab.setBackground(!s?pill(Color.WHITE,14):null); subtitle.setText(s?"how long has it been?":"what's coming up?"); }

    private void render(){
        list.removeAllViews(); ArrayList<Item> filtered=new ArrayList<>(); for(Item i:items) if(i.mode.equals(mode)) filtered.add(i); if(mode.equals("schedule")) filtered.sort(Comparator.comparing(a->a.date));
        if(filtered.isEmpty()){ TextView e=text(mode.equals("since")?"nothing here yet.\nadd something you keep forgetting 😋":"schedule empty.\nresponsibility has not found you yet.",17,MUTED,Typeface.NORMAL); e.setGravity(Gravity.CENTER); e.setPadding(dp(24),dp(80),dp(24),dp(80)); list.addView(e); return; }
        for(Item i:filtered) addCard(i);
    }

    private void addCard(Item item){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18),dp(17),dp(18),dp(16)); card.setBackground(roundRect(CARD,24,Color.rgb(230,230,234),1)); card.setElevation(dp(2));
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); LinearLayout left=new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); left.addView(text(item.name,19,TEXT,Typeface.BOLD));
        TextView little=text(mode.equals("since")?"last done "+prettyDate(item.date):(item.repeatDays>0?"repeats every "+item.repeatDays+" days":"one-time"),13,MUTED,Typeface.NORMAL); little.setPadding(0,dp(3),0,0); left.addView(little);
        TextView big=text(mainMetric(item),31,TEXT,Typeface.BOLD); big.setGravity(Gravity.END); top.addView(left,new LinearLayout.LayoutParams(0,-2,1)); top.addView(big,new LinearLayout.LayoutParams(-2,-2)); card.addView(top);
        TextView status=text(statusLine(item),14,isLate(item)?RED:(mode.equals("schedule")?BLUE:MUTED),Typeface.BOLD); status.setPadding(0,dp(14),0,dp(12)); card.addView(status);
        LinearLayout actions=new LinearLayout(this); Button main=smallButton(mode.equals("since")?"Done now":"Complete",TEXT,Color.WHITE); main.setOnClickListener(v->{ if(mode.equals("since")) item.date=LocalDate.now().toString(); else if(item.repeatDays>0) item.date=LocalDate.now().plusDays(item.repeatDays).toString(); else items.remove(item); saveItems(); render(); });
        Button edit=smallButton("Edit",Color.rgb(236,236,240),TEXT); edit.setOnClickListener(v->showItemDialog(item)); actions.addView(main,new LinearLayout.LayoutParams(0,dp(44),1)); Space gap=new Space(this); actions.addView(gap,new LinearLayout.LayoutParams(dp(8),1)); actions.addView(edit,new LinearLayout.LayoutParams(dp(92),dp(44))); card.addView(actions);
        card.setOnLongClickListener(v->{ new AlertDialog.Builder(this).setTitle("Delete \""+item.name+"\"?").setMessage("gone. reduced to atoms.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{items.remove(item);saveItems();render();}).show(); return true; });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(12)); list.addView(card,p);
    }

    private void showItemDialog(Item editing){
        boolean isEdit=editing!=null; final LocalDate[] chosen={isEdit?LocalDate.parse(editing.date):LocalDate.now()}; LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(6),dp(20),0);
        EditText name=new EditText(this); name.setHint("e.g. changed my sheets"); name.setText(isEdit?editing.name:""); name.setSingleLine(true); box.addView(name,new LinearLayout.LayoutParams(-1,dp(58)));
        Spinner type=new Spinner(this); ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Since","Schedule"}); type.setAdapter(ad); type.setSelection((isEdit?editing.mode:mode).equals("since")?0:1); box.addView(type);
        Button dateButton=new Button(this); dateButton.setAllCaps(false); dateButton.setText(formatDateButton(chosen[0])); dateButton.setOnClickListener(v->{LocalDate d=chosen[0]; new DatePickerDialog(this,(view,y,m,day)->{chosen[0]=LocalDate.of(y,m+1,day);dateButton.setText(formatDateButton(chosen[0]));},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();}); box.addView(dateButton);
        EditText repeat=new EditText(this); repeat.setHint("repeat every N days (0 = never)"); repeat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); repeat.setText(isEdit?String.valueOf(editing.repeatDays):"0"); box.addView(repeat,new LinearLayout.LayoutParams(-1,dp(54)));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(isEdit?"Edit thing":"Add thing").setView(box).setNegativeButton("Cancel",null).setPositiveButton(isEdit?"Save":"Add",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{ String n=name.getText().toString().trim(); if(n.isEmpty()){name.setError("give it a name bro");return;} String m=type.getSelectedItemPosition()==0?"since":"schedule"; int r=0; try{r=Integer.parseInt(repeat.getText().toString().trim());}catch(Exception ignored){} if(m.equals("since"))r=0; if(isEdit){editing.name=n;editing.mode=m;editing.date=chosen[0].toString();editing.repeatDays=r;}else items.add(new Item(UUID.randomUUID().toString(),n,m,chosen[0].toString(),r)); saveItems(); mode=m; updateTabs(); render(); dlg.dismiss(); })); dlg.show();
    }

    private String mainMetric(Item item){ LocalDate d=LocalDate.parse(item.date); long diff=ChronoUnit.DAYS.between(mode.equals("since")?d:LocalDate.now(),mode.equals("since")?LocalDate.now():d); if(mode.equals("since")){if(diff==0)return"today";if(diff==1)return"1 day";if(diff<14)return diff+" days";if(diff<60)return(diff/7)+" wk";if(diff<730)return(diff/30)+" mo";return(diff/365)+" yr";}else{if(diff==0)return"today";if(diff>0)return"+"+diff+"d";return diff+"d";} }
    private String statusLine(Item item){ LocalDate d=LocalDate.parse(item.date); if(mode.equals("since")){long days=ChronoUnit.DAYS.between(d,LocalDate.now()); if(days==0)return"freshly done ✨"; if(days<7)return"still respectable"; if(days<30)return"hmm. it's been a minute."; return"bro maybe do this 😭";} long days=ChronoUnit.DAYS.between(LocalDate.now(),d); if(days==0)return"due today"; if(days>0)return"due "+prettyDate(item.date); return"overdue by "+Math.abs(days)+" day"+(Math.abs(days)==1?"":"s"); }
    private boolean isLate(Item item){ return mode.equals("schedule")&&LocalDate.parse(item.date).isBefore(LocalDate.now()); }
    private String prettyDate(String iso){return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d"));}
    private String formatDateButton(LocalDate d){return d.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));}
    private Button smallButton(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(fg);b.setAllCaps(false);b.setBackground(pill(bg,15));return b;}
    private TextView text(String s,float size,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,style);return t;}
    private GradientDrawable pill(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable roundRect(int c,int r,int sc,int sw){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(sw),sc);return g;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void seed(){items.add(new Item("1","changed my sheets","since",LocalDate.now().minusDays(9).toString(),0));items.add(new Item("2","cleaned keyboard","since",LocalDate.now().minusDays(21).toString(),0));items.add(new Item("3","water plants","schedule",LocalDate.now().plusDays(2).toString(),5));items.add(new Item("4","haircut","schedule",LocalDate.now().plusDays(11).toString(),28));}
    private void saveItems(){JSONArray a=new JSONArray();try{for(Item i:items){JSONObject o=new JSONObject();o.put("id",i.id);o.put("name",i.name);o.put("mode",i.mode);o.put("date",i.date);o.put("repeatDays",i.repeatDays);a.put(o);}prefs.edit().putString("items",a.toString()).apply();}catch(Exception ignored){}}
    private void loadItems(){items.clear();try{JSONArray a=new JSONArray(prefs.getString("items","[]"));for(int x=0;x<a.length();x++){JSONObject o=a.getJSONObject(x);items.add(new Item(o.optString("id",UUID.randomUUID().toString()),o.optString("name","thing"),o.optString("mode","since"),o.optString("date",LocalDate.now().toString()),o.optInt("repeatDays",0)));}}catch(Exception ignored){}}
    static class Item{String id,name,mode,date;int repeatDays;Item(String id,String name,String mode,String date,int repeatDays){this.id=id;this.name=name;this.mode=mode;this.date=date;this.repeatDays=repeatDays;}}
}
