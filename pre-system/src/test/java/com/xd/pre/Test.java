package com.xd.pre;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {
        String a=
                "021700000010186332\n" +
                "021700000010186338\n" +
                "021700000010186342\n" +
                "021700000010186346\n" +
                "021700000010186349\n" +
                "021700000010111537\n" +
                "021700000010111547\n" +
                "021700000010111553\n" +
                "021700000010186354\n" +
                "021700000010111560\n" +
                "021700000010111567\n" +
                "021700000010186362\n" +
                "021700000010111573\n" +
                "021700000010186365\n" +
                "021700000010186371\n" +
                "021700000010186377\n" +
                "021700000010186383\n" +
                "021700000010186389\n" +
                "021700000010186395\n" +
                "021700000010111581\n" +
                "021700000010186403\n" +
                "021700000010186409\n" +
                "021700000010111589\n" +
                "021700000010111597\n" +
                "021700000010111605\n" +
                "021700000010186416\n" +
                "021700000010111612\n" +
                "021700000010111620\n" +
                "021700000010186423\n" +
                "021700000010111624\n" +
                "021700000010111632\n" +
                "021700000010111640\n" +
                "021700000010111647\n" +
                "021700000010111655\n" +
                "021700000010111663\n" +
                "021700000010186431\n" +
                "021700000010111671\n" +
                "021700000010186438\n" +
                "021700000010111676\n" +
                "021700000010111684\n" +
                "021700000010111692\n" +
                "021700000010186445\n" +
                "021700000010111700\n" +
                "021700000010186451\n" +
                "021700000010111707\n" +
                "021700000010186458\n" +
                "021700000010111715\n" +
                "021700000010111722\n" +
                "021700000010111728\n" +
                "021700000010111736\n" +
                "021700000010186465\n" +
                "021700000010111744\n" +
                "021700000010111751\n" +
                "021700000010111759\n" +
                "021700000010111767\n" +
                "021700000010186472\n" +
                "021700000010111775\n" +
                "021700000010111781\n" +
                "021700000010186479\n" +
                "021700000010186486\n" +
                "021700000010111788\n" +
                "021700000010111795\n" +
                "021700000010111802\n" +
                "021700000010111809\n" +
                "021700000010111815\n" +
                "021700000010186493\n" +
                "021700000010186500\n" +
                "021700000010111819\n" +
                "021700000010111823\n" +
                "021700000010111827\n" +
                "021700000010111831\n" +
                "021700000010111835\n" +
                "021700000010111841\n" +
                "021700000010111845\n" +
                "021700000010111852\n" +
                "021700000010111858\n" +
                "021700000010111864\n" +
                "021700000010111870\n" +
                "021700000010111876\n" +
                "021700000010111883\n" +
                "021700000010111889\n" +
                "021700000010111896\n" +
                "021700000010186507\n" +
                "021700000010111903\n" +
                "021700000010111911\n" +
                "021700000010111917\n" +
                "021700000010111924\n" +
                "021700000010111931\n" +
                "021700000010111945\n" +
                "021700000010111939\n" +
                "021700000010186514\n" +
                "021700000010186521\n" +
                "021700000010111952\n" +
                "021700000010111959\n" +
                "021700000010111966\n" +
                "021700000010111971\n" +
                "021700000010186527\n" +
                "021700000010111977\n" +
                "021700000010111983\n" +
                "021700000010111990\n" +
                "021700000010186532\n" +
                "021700000010186538\n" +
                "021700000010186546\n" +
                "021700000010186553\n" +
                "021700000010186560\n" +
                "021700000010186567\n" +
                "021700000010186575\n" +
                "021700000010186581\n" +
                "021700000010186588\n" +
                "021700000010186596\n" +
                "021700000010186603\n" +
                "021700000010186610\n" +
                "021700000010186616\n" +
                "021700000010186628\n" +
                "021700000010186622\n" +
                "021700000010186635\n" +
                "021700000010186642\n" +
                "021700000010186649\n" +
                "021700000010186656\n" +
                "021700000010186662\n" +
                "021700000010186669\n" +
                "021700000010186676\n" +
                "021700000010186683\n" +
                "021700000010186693\n" +
                "021700000010186700\n" +
                "021700000010186707\n" +
                "021700000010186713\n" +
                "021700000010186719\n" +
                "021700000010186725\n" +
                "021700000010186732\n" +
                "021700000010186737\n" +
                "021700000010186743\n" +
                "021700000010186749\n" +
                "021700000010186755\n" +
                "021700000010186761\n" +
                "021700000010186767\n" +
                "021700000010186773\n" +
                "021700000010186779\n" +
                "021700000010186784\n" +
                "021700000010186791\n" +
                "021700000010186797\n" +
                "021700000010186804\n" +
                "021700000010186812\n" +
                "021700000010186818\n" +
                "021700000010186825\n" +
                "021700000010186832\n" +
                "021700000010186838\n" +
                "021700000010186847\n" +
                "021700000010186854\n" +
                "021700000010186861\n" +
                "021700000010186867\n" +
                "021700000010186874\n" +
                "021700000010186880\n" +
                "021700000010186885\n" +
                "021700000010186891\n" +
                "021700000010186898\n" +
                "021700000010186904\n" +
                "021700000010186912\n" +
                "021700000010186919\n" +
                "021700000010186926\n" +
                "021700000010186937\n" +
                "021700000010186943\n" +
                "021700000010186949\n" +
                "021700000010186957\n" +
                "021700000010186964\n" +
                "021700000010186972\n" +
                "021700000010186979\n" +
                "021700000010186986\n" +
                "021700000010186991\n" +
                "021700000010186998\n" +
                "021700000010187005\n" +
                "021700000010187012\n" +
                "021700000010187019\n" +
                "021700000010187025\n" +
                "021700000010187031\n" +
                "021700000010187038\n" +
                "021700000010187046\n" +
                "021700000010187054\n" +
                "021700000010187061\n" +
                "021700000010187068\n" +
                "021700000010187074\n" +
                "021700000010187082\n" +
                "021700000010187089\n" +
                "021700000010187096\n" +
                "021700000010187102\n" +
                "021700000010187109\n" +
                "021700000010187116\n" +
                "021700000010187123\n" +
                "021700000010187130\n" +
                "021700000010187138\n" +
                "021700000010187145\n" +
                "021700000010187152\n" +
                "021700000010187159\n" +
                "021700000010187166\n" +
                "021700000010187172\n" +
                "021700000010187179\n" +
                "021700000010187187\n" +
                "021700000010187201\n" +
                "021700000010187194\n" +
                "021700000010187208\n" +
                "021700000010187213\n" +
                "021700000010187220\n" +
                "021700000010187227\n" +
                "021700000010187234\n" +
                "021700000010187240\n" +
                "021700000010187246\n" +
                "021700000010187253\n" +
                "021700000010187261\n" +
                "021700000010187268\n" +
                "021700000010187275\n" +
                "021700000010187289\n" +
                "021700000010187296\n" +
                "021700000010187303\n" +
                "021700000010187309\n" +
                "021700000010187317\n" +
                "021700000010187323\n" +
                "021700000010187329\n" +
                "021700000010187336\n" +
                "021700000010187343\n" +
                "021700000010187350\n" +
                "021700000010187357\n" +
                "021700000010187363\n" +
                "021700000010187372\n" +
                "021700000010187378\n" +
                "021700000010187386\n" +
                "021700000010187393\n" +
                "021700000010187400\n" +
                "021700000010187407\n" +
                "021700000010187415\n" +
                "021700000010187420\n" +
                "021700000010187426\n" +
                "021700000010187431\n" +
                "021700000010187439\n" +
                "021700000010187446\n" +
                "021700000010187454\n" +
                "021700000010187460\n" +
                "021700000010187468\n" +
                "021700000010187475\n" +
                "021700000010187481\n" +
                "021700000010187489\n" +
                "021700000010187495\n" +
                "021700000010187503\n" +
                "021700000010187509\n" +
                "021700000010187521\n" +
                "021700000010187515\n" +
                "021700000010187525\n" +
                "021700000010187532\n" +
                "021700000010187539\n" +
                "021700000010187546\n" +
                "021700000010187553\n" +
                "021700000010187568\n" +
                "021700000010187561\n" +
                "021700000010187581\n" +
                "021700000010187588\n" +
                "021700000010187592\n" +
                "021700000010187599\n" +
                "021700000010187606\n" +
                "021700000010187613\n" +
                "021700000010187628\n" +
                "021700000010187620\n" +
                "021700000010187635\n" +
                "021700000010187642\n" +
                "021700000010187649\n" +
                "021700000010187657\n" +
                "021700000010187663\n" +
                "021700000010187669\n" +
                "021700000010187675\n" +
                "021700000010187683\n" +
                "021700000010187689\n" +
                "021700000010187696\n" +
                "021700000010187703\n" +
                "021700000010187711\n" +
                "021700000010187719\n" +
                "021700000010187726\n" +
                "021700000010187733\n" +
                "021700000010187739\n" +
                "021700000010187746\n" +
                "021700000010187753\n" +
                "021700000010187760\n" +
                "021700000010187766\n" +
                "021700000010187773\n" +
                "021700000010187779\n" +
                "021700000010187786\n" +
                "021700000010187793\n" +
                "021700000010187800\n" +
                "021700000010187822\n" +
                "021700000010187830\n" +
                "021700000010187837\n" +
                "021700000010187843\n" +
                "021700000010187850\n" +
                "021700000010187875\n" +
                "021700000010187882\n" +
                "021700000010187889\n" +
                "021700000010187897\n" +
                "021700000010187904\n" +
                "021700000010187911\n" +
                "021700000010187918\n" +
                "021700000010187925\n" +
                "021700000010187932\n" +
                "021700000010187939\n" +
                "021700000010187947\n" +
                "021700000010187961\n" +
                "021700000010187954\n" +
                "021700000010187968\n" +
                "021700000010187974\n" +
                "021700000010187988\n" +
                "021700000010187980\n" +
                "021700000010187994\n" +
                "021700000010187999\n" +
                "021700000010188005\n" +
                "021700000010188012\n" +
                "021700000010188016\n" +
                "021700000010188022\n" +
                "021700000010188036\n" +
                "021700000010188030\n" +
                "021700000010188044\n" +
                "021700000010188051\n" +
                "021700000010188061\n" +
                "021700000010188067\n" +
                "021700000010188074\n" +
                "021700000010188081\n" +
                "021700000010188088\n" +
                "021700000010188095\n" +
                "021700000010188098\n" +
                "021700000010188104\n" +
                "021700000010188111\n" +
                "021700000010188118\n";
        String[] split = a.split("\n");
        OkHttpClient client = new OkHttpClient();
        for(int i =0;i<split.length;i++){
            String s = split[i];
            String[] s1 = s.split(" ");
            String cardNum = s1[0].trim();
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, String.format("api_key=8cec5243ade04ed3a02c5972bcda0d3f&app_version=4.0&warehouse=VIP_NH&client=wap&cardNos=%s&clientChannel=WAP",cardNum));
            Request request = new Request.Builder()
                    .url("https://mapi.vip.com/vips-mobile/rest/vps/wap/getvipcardstatus/v1?_=1659169395169")
                    .post(body)
                    .addHeader("cookie", "VIP_TANK=40344E836FBA768FD98D63D581EF158ECC2312FE;")
                    .build();
            Response response = client.newCall(request).execute();
            String string = response.body().string();
            String data = JSON.parseObject(string).getString("data");
            String vipCards = JSON.parseObject(data).getString("vipCards");
            List<JSONObject> jsonObjects = JSON.parseArray(vipCards, JSONObject.class);
            String status = jsonObjects.get(0).getString("status");
            if(Integer.valueOf(status)==1){
                System.out.println(s+"  "+"??????");
            }else if (Integer.valueOf(status)==2){
                System.out.println(s+"  "+"??????");
            }else {
                System.out.println(s+" ????????????");
            }

        }
    }
}
