package com.bookshop01.member.service;

import java.util.Map;

import org.springframework.dao.DataAccessException;

import com.bookshop01.member.vo.MemberVO;

public interface MemberService {
	public MemberVO login(Map  loginMap) throws Exception;
	public void addMember(MemberVO memberVO) throws Exception;
	public String overlapped(String id) throws Exception;
	public int removeMember(String member_id) throws DataAccessException;
}
