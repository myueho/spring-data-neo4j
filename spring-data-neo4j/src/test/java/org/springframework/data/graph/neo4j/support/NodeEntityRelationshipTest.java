package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class NodeEntityRelationshipTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testCreateRelationshipWithoutAnnotationOnSet() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        Node spouseNode=p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode();
        assertEquals(spouse.getPersistentState(), spouseNode);
        assertEquals(spouse, p.getSpouse());
    }

    @Test
    @Transactional
    public void testCreateRelationshipWithAnnotationOnSet() {
        Person p = persistedPerson("Michael", 35);
        Person mother = persistedPerson("Gabi", 60);
        p.setMother(mother);
        Node motherNode = p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("Person.mother"), Direction.OUTGOING).getEndNode();
        assertEquals(mother.getPersistentState(), motherNode);
        assertEquals(mother, p.getMother());
    }

    @Test
    @Transactional
    public void testDeleteRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        p.setSpouse(null);
        Assert.assertNull(p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING));
        Assert.assertNull(p.getSpouse());
    }

    @Test
    @Transactional
    public void testDeletePreviousRelationshipOnNewRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        Person friend = persistedPerson("Helga", 34);
        p.setSpouse(spouse);
        p.setSpouse(friend);
        assertEquals(friend.getPersistentState(), p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode());
        assertEquals(friend, p.getSpouse());
    }

    @Test
    @Transactional
    public void testCreateIncomingRelationshipWithAnnotationOnSet() {
        Person p = persistedPerson("David", 25);
        Person boss = persistedPerson("Emil", 32);
        p.setBoss(boss);
        assertEquals(boss.getPersistentState(), p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("boss"), Direction.INCOMING).getStartNode());
        assertEquals(boss, p.getBoss());
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testCircularRelationship() {
        Person p = persistedPerson("Michael", 35);
        p.setSpouse(p);
    }
    @Test
    @Transactional
    public void testSetOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = new Group().persist();
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        Relationship michaelRel = michael.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
        Relationship davidRel = david.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
        assertEquals(group.getPersistentState(), michaelRel.getStartNode());
        assertEquals(group.getPersistentState(), davidRel.getStartNode());
    }

    @Test
    @Transactional
    public void testGetOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = new Group().persist();
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        Collection<Person> personsFromGet = group.getPersons();
        assertEquals(persons, personsFromGet);
        Assert.assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
    }

    @Test
    @Transactional
    public void testAddToOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = new Group().persist();
        group.setPersons(new HashSet<Person>());
        group.getPersons().add(michael);
        group.getPersons().add(david);
        Collection<Person> personsFromGet = group.getPersons();
        assertEquals(new HashSet<Person>(Arrays.asList(david,michael)), personsFromGet);
        Assert.assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
    }

    @Test
    @Transactional
    public void testRemoveFromOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = new Group().persist();
        group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
        group.getPersons().remove(david);
        assertEquals(Collections.singleton(michael), group.getPersons());
    }

    @Test
    @Transactional
    public void testRelationshipGetEntities() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Person p3 = persistedPerson("Emil", 32);
        Friendship f2 = p.knows(p2);
        Friendship f3 = p.knows(p3);
        assertEquals(new HashSet<Friendship>(Arrays.asList(f2, f3)), IteratorUtil.addToCollection(p.getFriendships().iterator(), new HashSet<Friendship>()));
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testRelationshipSetEntitiesShouldThrowException() {
        Person p = persistedPerson("Michael", 35);
        p.setFriendships(new HashSet<Friendship>());
    }

    @Test
    @Transactional
    public void testOneToManyReadOnly() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = new Group().persist();
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        assertEquals(persons, IteratorUtil.addToCollection(group.getReadOnlyPersons().iterator(), new HashSet<Person>()));
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testOneToManyReadOnlyShouldThrowExceptionOnSet() {
        Group group = new Group().persist();
        group.setReadOnlyPersons(new HashSet<Person>());
    }

}
