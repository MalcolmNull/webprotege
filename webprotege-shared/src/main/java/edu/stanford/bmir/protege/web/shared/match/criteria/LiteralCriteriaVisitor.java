package edu.stanford.bmir.protege.web.shared.match.criteria;

import javax.annotation.Nonnull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 11 Jun 2018
 */
public interface LiteralCriteriaVisitor<R> {

    R visit(@Nonnull LiteralComponentCriteria criteria);

    R visit(@Nonnull LiteralLexicalValueNotInDatatypeLexicalSpaceCriteria criteria);
}
