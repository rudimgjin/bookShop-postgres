package com.bookshop01.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class TestService<Objec> {

	
	public String keyin() {
		System.out.println("aaaa");
		
		String result = "";
		
		try {
			
			//여기는 우리가 변경해야되는값
			String id = "himedia"; //발급된 계정
			String base = "https://api.testpayup.co.kr";
			String path = "/v2/api/payment/"+id+"/keyin2";
			
			String url = base+path;
			//url = https://api.testpayup.co.kr/v2/api/payment/himedia/keyin2
			
			
			//파라미터로 사용할 맵
			Map<String,String> map = new HashMap<String,String>();
			String signature="";
			//여기는 우리가 변경해야되는값 (여기까지)
			map.put("orderNumber","Test_12345");
			map.put("cardNo","5594100017038349");
			map.put("expireMonth","03");
			map.put("expireYear","23");
			map.put("birthday","900101");
			map.put("cardPw","12");
			map.put("amount","100");
			map.put("quota","0");
			map.put("itemName","아이템");
			map.put("userName","테스터");
			map.put("timestamp","20221010000000");
			
			//signature 값 만들기
			//SHA256 암호화를 써서 만들기.
			//{merchantId}|{orderNumber}|{amount}|{apiCertKey}|{timestamp}
			signature=encrypt(id+"|"+map.get("orderNumber")+"|"+map.get("amount")+"|ac805b30517f4fd08e3e80490e559f8e|"+map.get("timestamp"));
			
			System.out.println(signature);
			
			map.put("signature",signature);
			//여기는 우리가 변경 해야 하는값
		
			
			
			
			
			
			//JSON string데이터
			//맵을 JSON으로 변환 하는법 (라이브러리사용) , 사용하는 이유 편하니깐
			String param = "";
			
			ObjectMapper mapper = new ObjectMapper();
			param = mapper.writeValueAsString(map);
			
			//여기는 고정 값
			//OkHttp 사용
			//OkHttp = REST API , HTTP 통신을 간편하게 사용할 수 있도록 만들어진 라이브러리
			OkHttpClient client = new OkHttpClient();
			MediaType mediaType = MediaType.parse("application/json"); //application/json 이게 중요
			RequestBody body = RequestBody.create(mediaType,param);
			Request request = new Request.Builder().url(url)
					.post(body).addHeader("cache-control", "no-cache").build();
			//결과값 받기
			Response response = client.newCall(request).execute();
			result = response.body().string();
			
			//여기는 고정 값 (여기까지)
			
			
			
			//결과
			System.out.println("test = " + result);
			
			ObjectMapper resultMapper = new ObjectMapper();
			Map<String,Objec> resultMap =resultMapper.readValue(result, Map.class);
			
			System.out.println(resultMapper.toString());
			
			System.out.println("responseCode 응답코드 =" + resultMap.get("responseCode"));
			System.out.println("responseMsg 응답 메세지 =" + resultMap.get("responseMsg"));
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	public String encrypt(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes());

        return bytesToHex(md.digest());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}
