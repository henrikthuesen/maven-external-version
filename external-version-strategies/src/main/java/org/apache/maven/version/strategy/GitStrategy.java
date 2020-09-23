package org.apache.maven.version.strategy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Finds the nearest git tag.
 *
 * @author <a href="mailto:me@nowhere.no">Henrik Thuesen</a>
 */
@Component( role = ExternalVersionStrategy.class, hint = "git" )
public class GitStrategy
    implements ExternalVersionStrategy
{

    @Requirement
    private Logger log;

    private RevCommit pointsTo( RevWalk walk, Ref ref )
    {
        try
        {
            return (RevCommit) walk.parseTag( ref.getObjectId() ).getObject();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public String getVersion( MavenProject mavenProject ) throws ExternalVersionException
    {
        try
        {
            Repository repository = new FileRepositoryBuilder()
                .findGitDir( mavenProject.getBasedir() )
                .build();

            RevWalk walk = new RevWalk( repository );
            Map<String, Ref> tags = repository.getTags();
            Map<RevCommit, String> revTagsMap;
            revTagsMap = tags.entrySet()
                             .stream()
                             .collect( Collectors.toMap( e -> pointsTo( walk, e.getValue() ),
                                                         Map.Entry::getKey ) );
            walk.markStart( walk.parseCommit( repository.resolve( Constants.HEAD ) ) );

            for ( RevCommit rev : walk )
            {
                final String tagName = revTagsMap.get( rev );
                if ( tagName != null )
                {
                    return tagName;
                }
            }

        }
        catch ( IOException e )
        {
            throw new ExternalVersionException( "Failed to read repository file: [" + mavenProject.getBasedir() + "]",
                                                e );
        }

        return "NO_TAG";
    }
}
