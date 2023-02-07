package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.Comment;
import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RatingRepository.class);
    private static final AuditLogger AUDIT_LOGGER = AuditLogger.getAuditLogger(RatingRepository.class);


    private DataSource dataSource;

    public RatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createOrUpdate(Rating rating) {
        String query = "SELECT movieId, userId, rating FROM ratings WHERE movieId = " + rating.getMovieId() + " AND userID = " + rating.getUserId();
        String query2 = "update ratings SET rating = ? WHERE movieId = ? AND userId = ?";
        String query3 = "insert into ratings(movieId, userId, rating) values (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)
        ) {
            if (rs.next()) {
                Rating ratingFromDb = new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3));
                PreparedStatement preparedStatement = connection.prepareStatement(query2);
                preparedStatement.setInt(1, rating.getRating());
                preparedStatement.setInt(2, rating.getMovieId());
                preparedStatement.setInt(3, rating.getUserId());
                preparedStatement.executeUpdate();

                AUDIT_LOGGER.auditChange(new Entity(
                        "rating.update",
                        String.valueOf(rating.getMovieId()),
                "movieId=" + ratingFromDb.getMovieId() + ";" +
                        "userId=" + ratingFromDb.getUserId() + ";" +
                        "rating=" + ratingFromDb.getRating() + ";",
                        "movieId=" + rating.getMovieId() + ";" +
                        "userId=" + rating.getUserId() + ";" +
                        "rating=" + rating.getRating() + ";"
                ));

            } else {
                PreparedStatement preparedStatement = connection.prepareStatement(query3);
                preparedStatement.setInt(1, rating.getMovieId());
                preparedStatement.setInt(2, rating.getUserId());
                preparedStatement.setInt(3, rating.getRating());
                preparedStatement.executeUpdate();

                AUDIT_LOGGER.audit("Sucessfully created rating for movie " + rating.getMovieId());
            }
        } catch (SQLException e) {
           LOG.warn("Failed to create or update rating for movie {}", rating.getMovieId(), e);
        }
    }

    public List<Rating> getAll(String movieId) {
        List<Rating> ratingList = new ArrayList<>();
        String query = "SELECT movieId, userId, rating FROM ratings WHERE movieId = " + movieId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                ratingList.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            LOG.warn("Failed to get all ratings for moive {}", movieId, e);
        }
        return ratingList;
    }
}
