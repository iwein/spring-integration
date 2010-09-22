/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.transformer;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ObjectToMapTransformerTests {
	@SuppressWarnings("unchecked")
	@Test
	public void testObjectToSpelMapTransformer(){
		Employee employee = this.buildEmployee();
		EvaluationContext context = new StandardEvaluationContext(employee);
		ExpressionParser parser = new SpelExpressionParser();
		
		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		Message<Employee> message = MessageBuilder.withPayload(employee).build();
		Message<?> transformedMessage = transformer.transform(message);
		Map<String, Object> transformedMap = (Map<String, Object>) transformedMessage.getPayload();
		
		assertNotNull(transformedMap);
		for (String key : transformedMap.keySet()) {
			Expression expression = parser.parseExpression(key);
			//System.out.println("Testing: " + key);
			Object valueFromTheMap = transformedMap.get(key);
			Object valueFromExpression = expression.getValue(context);
			String packageNameOfValueType = valueFromTheMap.getClass().getPackage().getName();
			assertTrue(packageNameOfValueType.startsWith("java.lang"));
			assertEquals(valueFromTheMap, valueFromExpression);
		}		
	}
	@Test(expected=MessageTransformationException.class)
	public void testObjectToSpelMapTransformerWithCycle(){
		Employee employee = this.buildEmployee();
		Child child = new Child();	
		Person parent = employee.getPerson();
		parent.setChild(child);
		child.setParent(parent);
		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		Message<Employee> message = MessageBuilder.withPayload(employee).build();
		transformer.transform(message);
	}

	@SuppressWarnings("unchecked")
	public Employee buildEmployee(){
		Address companyAddress = new Address();
		companyAddress.setCity("Philadelphia");
		companyAddress.setStreet("1123 Main");
		companyAddress.setZip("12345");
		
		Map<String, Long[]> coordinates = new HashMap<String, Long[]>();
		coordinates.put("latitude", new Long[]{(long)1, (long)5, (long)13});
		coordinates.put("longitude", new Long[]{(long)156});
		companyAddress.setCoordinates(coordinates);
		
		Employee employee = new Employee();
		employee.setCompanyName("ABC Inc.");
		employee.setCompanyAddress(companyAddress);
		ArrayList departments = new ArrayList();
		departments.add("HR");
		departments.add("IT");
		employee.setDepartments(departments);
		
		Person person = new Person();
		person.setFname("Justin");
		person.setLname("Case");
		person.setAkaNames("Hard", "Use", "Beer");
		Address personAddress = new Address();
		personAddress.setCity("Philly");
		personAddress.setStreet("123 Main");
		List<String> listTestData = new ArrayList<String>();
		listTestData.add("hello");
		listTestData.add("blah");
		Map<String, List<String>> mapWithListTestData = new HashMap<String, List<String>>();
		mapWithListTestData.put("mapWithListTestData", listTestData);
		personAddress.setMapWithListData(mapWithListTestData);
		person.setAddress(personAddress);
		
		Map<String, Object> remarksA = new HashMap<String, Object>();
		Map<String, Object> remarksB = new HashMap<String, Object>();
		remarksA.put("foo", "foo");
		remarksA.put("bar", "bar");
		remarksB.put("baz", "baz");
		List<Map<String, Object>> remarks = new ArrayList<Map<String,Object>>();
		remarks.add(remarksA);
		remarks.add(remarksB);
		person.setRemarks(remarks);
		employee.setPerson(person);
		
		Map<String, Map<String, Object>> testMapData = new HashMap<String, Map<String, Object>>();
		
		Map<String, Object> internalMapA = new HashMap<String, Object>();
		internalMapA.put("foo", "foo");
		internalMapA.put("bar", "bar");
		Map<String, Object> internalMapB = new HashMap<String, Object>();
		internalMapB.put("baz", "baz");
		
		testMapData.put("internalMapA", internalMapA);
		testMapData.put("internalMapB", internalMapB);
		
		employee.setTestMapInMapData(testMapData);
		return employee;
	}
	
	public static class Employee{
		private List<String> departments;
		private String companyName;
		private Person person;
		private Address companyAddress;
		private Map<String, Map<String, Object>> testMapInMapData;
		public Map<String, Map<String, Object>> getTestMapInMapData() {
			return testMapInMapData;
		}
		public void setTestMapInMapData(
				Map<String, Map<String, Object>> testMapInMapData) {
			this.testMapInMapData = testMapInMapData;
		}
		public String getCompanyName() {
			return companyName;
		}
		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}
		public Person getPerson() {
			return person;
		}
		public void setPerson(Person person) {
			this.person = person;
		}
		public Address getCompanyAddress() {
			return companyAddress;
		}
		public void setCompanyAddress(Address companyAddress) {
			this.companyAddress = companyAddress;
		}
		public List<String> getDepartments() {
			return departments;
		}
		public void setDepartments(List<String> departments) {
			this.departments = departments;
		}
	}
	
	public static class Person{
		private String fname;
		private String lname;
		private String[] akaNames;
		private List<Map<String, Object>> remarks;
		private Child child;
		public Child getChild() {
			return child;
		}
		public void setChild(Child child) {
			this.child = child;
		}
		public List<Map<String, Object>> getRemarks() {
			return remarks;
		}
		public void setRemarks(List<Map<String, Object>> remarks) {
			this.remarks = remarks;
		}
		private Address address;
		public String[] getAkaNames() {
			return akaNames;
		}
		public void setAkaNames(String... akaNames) {
			this.akaNames = akaNames;
		}
		public String getFname() {
			return fname;
		}
		public void setFname(String fname) {
			this.fname = fname;
		}
		public String getLname() {
			return lname;
		}
		public void setLname(String lname) {
			this.lname = lname;
		}
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
	}
	
	public static class Address{
		private String street;
		private String city;
		private String zip;
		private Map<String, List<String>> mapWithListData;
		private Map<String, Long[]> coordinates;
		public Map<String, List<String>> getMapWithListData() {
			return mapWithListData;
		}
		public void setMapWithListData(Map<String, List<String>> mapWithListData) {
			this.mapWithListData = mapWithListData;
		}
		public String getStreet() {
			return street;
		}
		public void setStreet(String street) {
			this.street = street;
		}
		public String getCity() {
			return city;
		}
		public void setCity(String city) {
			this.city = city;
		}
		public String getZip() {
			return zip;
		}
		public void setZip(String zip) {
			this.zip = zip;
		}
		public Map<String, Long[]> getCoordinates() {
			return coordinates;
		}
		public void setCoordinates(Map<String, Long[]> coordinates) {
			this.coordinates = coordinates;
		}
	}
	
	public static class Child {
		private Person parent;

		public Person getParent() {
			return parent;
		}

		public void setParent(Person parent) {
			this.parent = parent;
		}
	}
}
